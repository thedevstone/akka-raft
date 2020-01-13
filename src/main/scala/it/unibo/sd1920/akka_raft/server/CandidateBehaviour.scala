package it.unibo.sd1920.akka_raft.server

import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.raft.{AppendEntries, RequestVote, RequestVoteResult}
import it.unibo.sd1920.akka_raft.server.ServerActor.SchedulerTick
import it.unibo.sd1920.akka_raft.utils.NetworkConstants

private trait CandidateBehaviour {
  this: ServerActor =>

  private var voteCounter: Int = 1

  protected def candidateBehaviour: Receive = clusterBehaviour orElse {
    case SchedulerTick => restart()
    case req: RequestVoteResult => handleVoteResult(req)
    case requestVote: RequestVote => handleRequestVote(requestVote)
    case _ =>
  }

  private def handleRequestVote(requestVote: RequestVote): Unit = {
    requestVote match {
      case RequestVote(candidateTerm, _, _, _) if candidateTerm <= currentTerm => sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case RequestVote(candidateTerm, _, lastLogTerm, lastLogIndex) if checkLogBehind(lastLogTerm, lastLogIndex) =>
        voteForAnotherCandidate(candidateTerm)
      case RequestVote(candidateTerm, _, _, _) => becomingFollower(candidateTerm)
        sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case _ =>
    }
  }

  private def checkLogBehind(lastLogTerm: Int, lastLogIndex: Int): Boolean = {
    lastLogTerm > currentTerm && lastLogIndex > serverLog.lastIndex
  }

  private def handleVoteResult(result: RequestVoteResult): Unit = result match {
    case RequestVoteResult(_, followerTerm) if followerTerm > currentTerm => becomingFollower(followerTerm)
    case RequestVoteResult(result, followerTerm) if result && followerTerm == currentTerm => voteCounter += 1
      if (voteCounter > (NetworkConstants.numberOfServer / 2)) becomingLeader()
    case _ =>
  }

  private def voteForAnotherCandidate(term: Int) {
    becomingFollower(term)
    votedFor = Some(sender().path.name)
    sender() ! RequestVoteResult(voteGranted = true, currentTerm)
  }

  private def becomingFollower(term: Int) {
    currentTerm = term
    context.become(followerBehaviour)
    startTimer()
  }

  private def becomingLeader(): Unit = {
    val lastEntry: Option[Entry[BankCommand]] = serverLog.getLastEntry
    servers.filter(serverRef => serverRef._2 != self).foreach(
      server => server._2 ! AppendEntries(currentTerm, if (lastEntry.isEmpty) None else serverLog.getPreviousEntry(lastEntry.get), lastEntry, lastCommittedIndex))
    context.become(leaderBehaviour)
    startTimer()
  }


  private def restart() {
    currentTerm += 1
    voteCounter = 1
    startTimer()
  }
}
