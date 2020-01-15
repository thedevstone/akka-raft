package it.unibo.sd1920.akka_raft.server

import it.unibo.sd1920.akka_raft.model.BankStateMachine.{ApplyCommand, BankCommand}
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.protocol.{AppendEntries, AppendEntriesResult, ClientRequest, RequestVote}
import it.unibo.sd1920.akka_raft.server.ServerActor.SchedulerTick


private trait LeaderBehaviour {
  this: ServerActor =>

  private var followersStatusMap: Map[String, FollowerStatus] = Map()

  protected def leaderBehaviour: Receive = controlBehaviour orElse {
    //FROM CLIENT
    case req: ClientRequest => handleRequest(req)
    //FROM SERVER
    case SchedulerTick => heartbeatTimeout()
    case AppendEntriesResult(success, matchIndex) => handleAppendResult(sender().path.name, success, matchIndex)
    case RequestVote(candidateTerm, _, _, _) if candidateTerm > currentTerm =>
    //case StateMachineResult =>
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

  //FROM FOLLOWER
  private def handleAppendResult(name: String, success: Boolean, matchIndex: Int): Unit = {
    val followerStatus = followersStatusMap(name)
    if (success) { //
      followersStatusMap = followersStatusMap + (name -> FollowerStatus(matchIndex + 1, matchIndex))
      if (followerStatus.nextIndexToSend <= serverLog.lastIndex) {
        val entryToSend = serverLog.getEntryAtIndex(followerStatus.nextIndexToSend)
        sender() ! AppendEntries(currentTerm, serverLog.getPreviousEntry(entryToSend.get), entryToSend, lastCommittedIndex)
      }
    } else { //Leader Consistency check
      followersStatusMap = followersStatusMap + (name -> FollowerStatus(followerStatus.nextIndexToSend - 1, matchIndex))
    }
  }

  protected def leaderPreBecome(): Unit = {
    servers.keys.foreach(name => followersStatusMap = followersStatusMap + (name -> FollowerStatus(serverLog.lastIndex, -1)))
  }
}

case class FollowerStatus(nextIndexToSend: Int, lastMatchIndex: Int) {
  assert(nextIndexToSend >= 0)
  assert(lastMatchIndex >= 0)
}
