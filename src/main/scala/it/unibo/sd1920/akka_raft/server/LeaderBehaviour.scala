package it.unibo.sd1920.akka_raft.server

import it.unibo.sd1920.akka_raft.model.Bank.BankTransactionResult
import it.unibo.sd1920.akka_raft.model.BankStateMachine.{ApplyCommand, BankCommand}
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.protocol._
import it.unibo.sd1920.akka_raft.server.ServerActor.{SchedulerTick, StateMachineResult}
import it.unibo.sd1920.akka_raft.utils.ServerRole

private trait LeaderBehaviour {
  this: ServerActor =>

  private var followersStatusMap: Map[String, FollowerStatus] = Map()

  protected def leaderBehaviour: Receive = controlBehaviour orElse MessageInterceptor({
    //FROM CLIENT
    case req: ClientRequest => handleRequest(req)
    //FROM SERVER
    case SchedulerTick => heartbeatTimeout()
    case AppendEntriesResult(success, matchIndex, term) => handleAppendResult(sender().path.name, success, matchIndex, term)
    case requestVote: RequestVote => handleRequestVote(requestVote)
    case result: StateMachineResult => handleStateMachineResult(result.indexAndResult)
    case AppendEntries(term, _, _, _) => checkBehindTerm(term)
    case _ =>
  })

  //FROM CLIENT
  /**
   * Handles ClientRequest message
   * <p>
   * If the request id is present then it is immediately executed by the '''state machine''' . If it is a new request then it is
   * appended in the server log.
   *
   * @param req the request from client
   */
  private def handleRequest(req: ClientRequest): Unit = {
    val requestIndex: Option[Int] = serverLog.getIndexFromReqId(req.requestID)
    if (serverLog.isReqIdCommitted(req.requestID)) {
      stateMachineActor ! ApplyCommand(new Entry[BankCommand](req.command, currentTerm, requestIndex.get, req.requestID))
      lastApplied = requestIndex.get
    } else if (requestIndex.isEmpty) {
      val entry = new Entry[BankCommand](req.command, currentTerm, serverLog.size, req.requestID)
      serverLog.insertEntry(entry)
    }
  }

  //EVENTS
  /**
   * Handles Timeout event.
   * <p>
   * When a timeout triggers then an '''heartbeat''' is sent to followers. If previous index is empty then an empty heartbeat is sent
   *
   */
  private def heartbeatTimeout(): Unit = {
    servers.toStream.filter(s => s._2 != self).foreach(e => {
      followersStatusMap(e._1).nextIndexToSend match {
        case 0 => e._2 ! AppendEntries(currentTerm, None, None, serverLog.getCommitIndex)
        case nextIndexToSend => e._2 ! AppendEntries(currentTerm, serverLog.getEntryAtIndex(nextIndexToSend - 1), None, serverLog.getCommitIndex)
      }
    })
  }

  //RESPONSES FROM FOLLOWERS
  /**
   * Handles AppendEntriesResult message
   * <p>
   * When a '''positive response''' arrives from followers then the leader has to:
   *
   *    - retrieve follower info from map
   *    - check if the majority of the followers share the same new entry/entries
   *    - check if the last entry to commit is from the leader term -> safe to commit
   *    - check if can send new entries
   *    - sends new entries and sends to follower the new commit index so they can commit as soon as they receive the message
   * <p>
   * When a '''negative response''' arrives from followers then the leader has to perform the '''consistency check''' and '''log repair'''
   *
   * @param name       the server name
   * @param success    if request succeeded
   * @param matchIndex the match index in log
   * @param term       the follower term
   */
  private def handleAppendResult(name: String, success: Boolean, matchIndex: Int, term: Int): Unit = {
    checkBehindTerm(term)
    if (currentRole == ServerRole.LEADER) {
      if (success) {
        followersStatusMap = followersStatusMap + (name -> FollowerStatus(matchIndex + 1, matchIndex))
        val followerStatus = followersStatusMap(name)
        val indexToCommit = getIndexToCommit
        if (checkCommitFromEarlierTerm(indexToCommit)) {
          callCommit(indexToCommit)
        }
        if (followerStatus.nextIndexToSend <= serverLog.lastIndex) {
          val entryToSend = serverLog.getEntryAtIndex(followerStatus.nextIndexToSend)
          sender() ! AppendEntries(currentTerm, serverLog.getPreviousEntry(entryToSend.get), entryToSend, serverLog.getCommitIndex)
        }
      } else { //Leader Consistency check
        val followerStatus = followersStatusMap(name)
        followersStatusMap = followersStatusMap + (name -> FollowerStatus(followerStatus.nextIndexToSend - 1, matchIndex))
      }
    }
  }

  /**
   * Get the index that has to be committed. An index can be committed if the majority of followers share the same index.
   *
   * @return the log index until all server agree with
   */
  private def getIndexToCommit: Int = {
    var commitIndexCounter: Int = serverLog.getCommitIndex - 1
    val matchIndexes = followersStatusMap.values.map(e => e.lastMatchIndex)
    var greaterMatches = 0 //number of servers whose matchIndex is greater than lastCommitIndex. They have to be the majority to be committed
    do {
      commitIndexCounter += 1
      greaterMatches = matchIndexes.count(m => m > commitIndexCounter)
    } while (greaterMatches >= HALF_FOLLOWERS_NUMBER) // Remember! I do not count myself because i'm the leader
    commitIndexCounter
  }

  /**
   * Very important check that prohibits the leader to commit an entry appended by another leader.
   * <p>
   * This check assures RAFT safety and prevents inconsistencies caused by strange and rare interactions.
   *
   * @param commitIndex actual index to commit
   * @return true if the leader can commit
   */
  private def checkCommitFromEarlierTerm(commitIndex: Int): Boolean = {
    if (commitIndex != -1) {
      val entry = serverLog.getEntryAtIndex(commitIndex).get
      !(entry.term < currentTerm)
    } else {
      false
    }
  }

  //RESULT FROM STATE MACHINE
  /**
   * Handles StateMachineResult message
   * <p>
   * When the state machine completes a request then it sends the result to server.
   *
   * @param indexAndResult the tuple containing index in log of the request and the request's result
   */
  private def handleStateMachineResult(indexAndResult: (Int, BankTransactionResult)): Unit = {
    val index = indexAndResult._1
    val result = indexAndResult._2
    val reqID = serverLog.getEntryAtIndex(index).get.requestId
    clients.last._2 ! RequestResult(reqID, result.isSucceeded, result.balance)
  }

  /**
   * Check if the leader is behind an other server.
   * <p>
   * This appends when a server crashes or it is unreachable. This create a partition in the cluster. If a new server is
   * elected in the other partition then all servers in that partition share the same '''term'''. If then this server came up
   * it could receive message with an updated term.
   *
   * @param term the most updated term to check
   */
  private def checkBehindTerm(term: Int): Unit = {
    if (term > currentTerm) becomingFollower(term)
  }

  //INITIALIZATION
  protected def followerStateInitialization(): Unit = {
    if (serverLog.lastIndex == -1) {
      servers.keys.filter(name => name != self.path.name).foreach(name => followersStatusMap = followersStatusMap + (name -> FollowerStatus(0, -1)))
    } else {
      servers.keys.filter(name => name != self.path.name).foreach(name => followersStatusMap = followersStatusMap + (name -> FollowerStatus(serverLog.lastIndex, -1)))
    }
  }
}

/**
 * Represents follower status in leader server.
 *
 * @param nextIndexToSend the next index to send to follower
 * @param lastMatchIndex  the last match index by follower
 */
case class FollowerStatus(nextIndexToSend: Int, lastMatchIndex: Int) {
  assert(nextIndexToSend >= 0)
  assert(lastMatchIndex >= -1)
}
