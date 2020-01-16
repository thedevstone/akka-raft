package it.unibo.sd1920.akka_raft.server


import akka.actor.ActorRef
import it.unibo.sd1920.akka_raft.model.ServerVolatileState
import it.unibo.sd1920.akka_raft.protocol._
import it.unibo.sd1920.akka_raft.protocol.GuiControlMessage.GuiServerState
import it.unibo.sd1920.akka_raft.server.ServerActor.SchedulerTick
import it.unibo.sd1920.akka_raft.utils.ServerRole


private trait FollowerBehaviour {
  this: ServerActor =>

  private var leaderRef: Option[ActorRef] = None

  protected def followerBehaviour: Receive = controlBehaviour orElse {
    case SchedulerTick => followerTimeout()
    case requestVote: RequestVote => handleRequestVote(requestVote)
    case appendEntry: AppendEntries => handleAppendEntries(appendEntry)
    case ClientRequest(requestID, _) => sender() ! Redirect(requestID, leaderRef)
    case _ =>
  }

  private def followerTimeout(): Unit = {
    logWithRole("Timeout")
    leaderRef = None
    currentTerm += 1
    context.become(candidateBehaviour)
    broadcastMessage(RequestVote(currentTerm, self, serverLog.lastTerm, serverLog.lastIndex))
    startTimeoutTimer()
    currentRole = ServerRole.CANDIDATE
  }

  private def handleRequestVote(requestVote: RequestVote): Unit = {
    checkAndUpdateTerm(requestVote.candidateTerm)

    requestVote match {
      case RequestVote(candidateTerm, _, _, _) if candidateTerm < currentTerm => sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case RequestVote(_, _, lastLogTerm, lastLogIndex) if votedFor.isEmpty && checkElectionRestriction(lastLogTerm, lastLogIndex) => votedFor = Some(sender().path.name)
        sender() ! RequestVoteResult(voteGranted = true, currentTerm)
      case RequestVote(_, _, _, _) => sender() ! RequestVoteResult(voteGranted = false, currentTerm)
    }
    startTimeoutTimer()
  }

  private def handleAppendEntries(appendEntry: AppendEntries): Unit = {
    leaderRef = Some(sender())
    checkAndUpdateTerm(appendEntry.leaderTerm)
    logWithRole("Messaggio arrivato" + appendEntry)
    appendEntry match {

      //caso rifiuto append da leader più indietro di me
      case AppendEntries(leaderTerm, _, _, _) if leaderTerm < currentTerm =>
        sender() ! AppendEntriesResult(success = false, -1)

      //caso leader manda prima entry del log.
      case AppendEntries(_, previousEntry, entry, leaderLastCommit) if previousEntry.isEmpty && entry.nonEmpty =>
        callCommit(Math.min(serverLog.lastIndex, leaderLastCommit))
        sender() ! AppendEntriesResult(serverLog.putElementAtIndex(entry.get), 0) //TODO MODIFICATO LAST MATCH IN 0

      //caso leader ha log vuoto e manda append con sia prev che entry vuote. Devo ritornare SEMPRE true
      case AppendEntries(_, previousEntry, entry, _) if previousEntry.isEmpty && entry.isEmpty =>
        sender() ! AppendEntriesResult(success = true, -1)

      //caso non ho prev entry nel log. Rispondi false indipendentemente da se entry è empty o meno
      case AppendEntries(_, previousEntry, _, _) if !serverLog.contains(previousEntry.get) =>
        sender() ! AppendEntriesResult(success = false, -1)

      //caso prev entry presente nel log. Se ho entry da appendere lo faccio,
      case AppendEntries(_, previousEntry, entry, leaderLastCommit) if entry.nonEmpty =>
        callCommit(Math.min(serverLog.lastIndex, leaderLastCommit))
        sender() ! AppendEntriesResult(serverLog.putElementAtIndex(entry.get), previousEntry.get.index + 1) //TODO errore +1?? //TODO MODIFICATO LAST MATCH IN +1

      // altrimenti ritorno solo true
      case AppendEntries(_, previousEntry, _, leaderLastCommit) => callCommit(Math.min(serverLog.lastIndex, leaderLastCommit))
        sender() ! AppendEntriesResult(success = true, previousEntry.get.index)

      case _ =>
    }
    clients.last._2 ! GuiServerState(ServerVolatileState(currentRole, serverLog.getCommitIndex, lastApplied, votedFor, currentTerm, 1, 1, serverLog.getEntries)) //TODO da cancellare

    startTimeoutTimer()
  }

  private def checkAndUpdateTerm(term: Int): Unit = {
    if (term > currentTerm) {
      currentTerm = term
      votedFor = None
    }
  }
}
