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

  //FROM OTHER LEADER
  private def checkBehindTerm(term: Int): Unit = {
    if (term > currentTerm) becomingFollower(term) //TODO EasyVersion
  }

  //FROM STATE MACHINE
  private def handleStateMachineResult(indexAndResult: (Int, BankTransactionResult)): Unit = {
    val index = indexAndResult._1
    val result = indexAndResult._2
    val reqID = serverLog.getEntryAtIndex(index).get.requestId
    clients.last._2 ! RequestResult(reqID, result.isSucceeded, result.balance) //TODO come mai?
  }

  //FROM CLIENT
  private def handleRequest(req: ClientRequest): Unit = {
    val i: Option[Int] = serverLog.getIndexFromReqId(req.requestID)
    if (serverLog.isReqIdCommitted(req.requestID)) {
      stateMachineActor ! ApplyCommand(new Entry[BankCommand](req.command, currentTerm, i.get, req.requestID))
    } else if (i.isEmpty) {
      val entry = new Entry[BankCommand](req.command, currentTerm, serverLog.size, req.requestID)
      serverLog.putElementAtIndex(entry)
    }
  }

  //FROM SELF
  private def heartbeatTimeout(): Unit = {
    servers.toStream.filter(s => s._2 != self).foreach(e => {
      followersStatusMap(e._1).nextIndexToSend match {
        case 0 => e._2 ! AppendEntries(currentTerm, None, None, serverLog.getCommitIndex)
        case nextIndexToSend => e._2 ! AppendEntries(currentTerm, serverLog.getEntryAtIndex(nextIndexToSend - 1), None, serverLog.getCommitIndex)
      }
    })
  }

  //FROM CANDIDATE
  private def handleRequestVote(requestVote: RequestVote): Unit = {
    requestVote match {
      case RequestVote(candidateTerm, _, _, _) if candidateTerm <= currentTerm => sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case RequestVote(candidateTerm, _, lastLogTerm, lastLogIndex) if checkElectionRestriction(lastLogTerm, lastLogIndex) =>
        voteForApplicantCandidate(candidateTerm)
      case RequestVote(candidateTerm, _, _, _) => becomingFollower(candidateTerm)
        sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case _ =>
    }
  }

  private def voteForApplicantCandidate(term: Int) {
    becomingFollower(term)
    votedFor = Some(sender().path.name)
    sender() ! RequestVoteResult(voteGranted = true, currentTerm)
  }

  private def becomingFollower(term: Int) {
    currentTerm = term
    context.become(followerBehaviour)
    startTimeoutTimer()
    currentRole = ServerRole.FOLLOWER
  }

  //FROM FOLLOWER
  private def handleAppendResult(name: String, success: Boolean, matchIndex: Int, term: Int): Unit = {
    checkBehindTerm(term)
    val followerStatus = followersStatusMap(name)
    if (success) {
      followersStatusMap = followersStatusMap + (name -> FollowerStatus(matchIndex + 1, matchIndex))
      val indexToCommit = getIndexToCommit
      if (checkCommitFromEarlierTerm(indexToCommit))
        callCommit(indexToCommit)
      if (followerStatus.nextIndexToSend <= serverLog.lastIndex) {
        val entryToSend = serverLog.getEntryAtIndex(followerStatus.nextIndexToSend)
        sender() ! AppendEntries(currentTerm, serverLog.getPreviousEntry(entryToSend.get), entryToSend, serverLog.getCommitIndex)
      }
    } else { //Leader Consistency check
      followersStatusMap = followersStatusMap + (name -> FollowerStatus(followerStatus.nextIndexToSend - 1, matchIndex))
    }
  }

  private def getIndexToCommit: Int = {
    var commitIndexCounter: Int = serverLog.getCommitIndex - 1
    val matchIndexes = followersStatusMap.values.map(e => e.lastMatchIndex)
    var greaterMatches = 0 //number of servers whose matchIndex is greater than lastCommitIndex. They have to be the majority to be committed
    do {
      commitIndexCounter += 1
      greaterMatches = matchIndexes.count(m => m > commitIndexCounter)
    } while (greaterMatches >= SERVERS_MAJORITY - 1) // -1 because I do not count myself
    commitIndexCounter
  }

  private def checkCommitFromEarlierTerm(commitIndex: Int): Boolean = {
    if (commitIndex != -1) {
      val entry = serverLog.getEntryAtIndex(commitIndex).get
      !(entry.term < currentTerm)
    } else {
      false
    }

  }

  protected def leaderPreBecome(): Unit = {
    if (serverLog.lastIndex == -1) {
      servers.keys.filter(name => name != self.path.name).foreach(name => followersStatusMap = followersStatusMap + (name -> FollowerStatus(0, -1)))
    } else {
      servers.keys.filter(name => name != self.path.name).foreach(name => followersStatusMap = followersStatusMap + (name -> FollowerStatus(serverLog.lastIndex, -1)))
    }
  }
}

case class FollowerStatus(nextIndexToSend: Int, lastMatchIndex: Int) {
  assert(nextIndexToSend >= 0)
  assert(lastMatchIndex >= -1)
}
