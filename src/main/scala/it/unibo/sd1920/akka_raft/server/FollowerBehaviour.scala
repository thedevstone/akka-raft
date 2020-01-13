package it.unibo.sd1920.akka_raft.server


import akka.actor.ActorRef
import it.unibo.sd1920.akka_raft.raft._
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientRequest, SchedulerTick}


private trait FollowerBehaviour {
  this: ServerActor =>

  private var leaderRef : Option[ActorRef] = None

  protected def followerBehaviour: Receive =  clusterBehaviour orElse {
    case SchedulerTick => becomingCandidate()
    case requestVote: RequestVote => handleRequestVote(requestVote)
    case appendEntry: AppendEntries => leaderRef = Some(sender())
      handleAppendEntries(appendEntry)
    case ClientRequest(_, _) => sender() ! Redirect(leaderRef)
    case _ =>
  }

  def becomingCandidate(): Unit = {
    leaderRef = None
    currentTerm += 1
    context.become(candidateBehaviour)
    startTimer()
  }

  def handleRequestVote(requestVote: RequestVote): Unit =  {
    updateTerm(requestVote.candidateTerm)

    requestVote match{
      case RequestVote(candidateTerm,_ ,_ ,_) if candidateTerm < currentTerm => sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case RequestVote(_, _,lastLogTerm,lastLogIndex) if votedFor.isEmpty && checkLogBehind(lastLogTerm,lastLogIndex) => votedFor = Some(sender().path.name)
        sender() ! RequestVoteResult(voteGranted = true, currentTerm)
      case RequestVote(_, _, _, _) => sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case _ =>
    }
    startTimer()
  }

  def handleAppendEntries(appendEntry: AppendEntries): Unit = {
    updateTerm(appendEntry.leaderTerm)

    appendEntry match{
      //caso heartbeat (da rimuovere)
      case AppendEntries(_, _, entry, _) if entry.isEmpty => sender() !

      //caso rifiuto append da leader più indietro di me
      case AppendEntries(leaderTerm, _, _, _) if leaderTerm < currentTerm => sender() ! AppendEntriesResult(false)

      //caso leader manda prima entry del log.
      case AppendEntries(_, previousEntry, entry, leaderLastCommit) if previousEntry.isEmpty && entry.nonEmpty => callCommit(Math.min(serverLog.getCommitIndex, leaderLastCommit))
        sender() ! AppendEntriesResult(serverLog.putElementAtIndex(entry.get))

      //caso leader ha log vuoto e manda append con sia prev che entry vuote. Devo ritornare SEMPRE true
      case AppendEntries(_, previousEntry, entry, _)  if previousEntry.isEmpty && entry.isEmpty => sender() ! AppendEntriesResult(true)


      //caso non ho prev entry nel log. Rispondi false indipendentemente da se entry è empty o meno
      case AppendEntries(_, previousEntry, _, _) if !serverLog.contains(previousEntry.get) => sender() ! AppendEntriesResult(false)

      //caso prev entry presente nel log. Se ho entry da appendere lo faccio,
      case AppendEntries(_, _, entry, leaderLastCommit) if entry.nonEmpty => callCommit(Math.min(serverLog.getCommitIndex, leaderLastCommit))
        sender() ! AppendEntriesResult(serverLog.putElementAtIndex(entry.get))

      // altrimenti ritorno solo true
      case AppendEntries(_, _, _, leaderLastCommit) => callCommit(Math.min(serverLog.getCommitIndex, leaderLastCommit))
        sender() ! AppendEntriesResult(true)

      case _ =>
    }
    startTimer()
  }

  def updateTerm(term: Int): Unit = {
    if (term > currentTerm){
      currentTerm = term
      votedFor = None
    }
  }

  def callCommit(index: Int) {
    serverLog.commit(index)
  }

  def checkLogBehind(lastLogTerm: Int, lastLogIndex: Int): Boolean = {
    lastLogTerm >= currentTerm && lastLogIndex >= serverLog.lastIndex
  }

}
