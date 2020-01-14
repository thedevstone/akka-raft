package it.unibo.sd1920.akka_raft.server

import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.protocol.{AppendEntries, RequestVote, RequestVoteResult}
import it.unibo.sd1920.akka_raft.server.ServerActor.SchedulerTick

private trait CandidateBehaviour {
  this: ServerActor =>

  private var voteCounter: Int = 1

  protected def candidateBehaviour: Receive = controlBehaviour orElse {
    case SchedulerTick => electionTimeout()
    case requestVote: RequestVote => handleRequestVote(requestVote)
    case requestResult: RequestVoteResult => handleVoteResult(requestResult)
    //TODO AppendEntries almeno guarda il term
    case _ =>
  }

  private def handleRequestVote(requestVote: RequestVote): Unit = {
    requestVote match {
      case RequestVote(candidateTerm, _, _, _) if candidateTerm <= currentTerm => sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case RequestVote(candidateTerm, _, lastLogTerm, lastLogIndex) if electionRestriction(lastLogTerm, lastLogIndex) =>
        voteForApplicantCandidate(candidateTerm)
      case RequestVote(candidateTerm, _, _, _) => becomingFollower(candidateTerm)
        sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case _ =>
    }
  }

  private def handleVoteResult(result: RequestVoteResult): Unit = result match {
    case RequestVoteResult(_, followerTerm) if followerTerm > currentTerm => becomingFollower(followerTerm)
    case RequestVoteResult(result, followerTerm) if result && followerTerm == currentTerm => voteCounter += 1
      if (voteCounter >= SERVERS_MAJORITY) becomingLeader()
    case _ =>
  }

  private def voteForApplicantCandidate(term: Int) {
    becomingFollower(term)
    votedFor = Some(sender().path.name)
    sender() ! RequestVoteResult(voteGranted = true, currentTerm)
  }

  private def becomingFollower(term: Int) {
    currentTerm = term
    context.become(followerBehaviour)
    voteForMyself()
    startTimeoutTimer()
  }

  private def electionRestriction(lastLogTerm: Int, lastLogIndex: Int): Boolean = {
    lastLogTerm > currentTerm && lastLogIndex > serverLog.lastIndex
  }

  private def becomingLeader(): Unit = {
    val lastEntry: Option[Entry[BankCommand]] = serverLog.getLastEntry
    broadcastMessage(AppendEntries(currentTerm,
      if (lastEntry.isEmpty) None else serverLog.getPreviousEntry(lastEntry.get), lastEntry, lastCommittedIndex))
    context.become(leaderBehaviour)
    voteForMyself()
    startHeartbeatTimer()
    leaderPreBecome()
  }

  private def electionTimeout(): Unit = {
    currentTerm += 1
    voteForMyself()
    broadcastMessage(RequestVote(currentTerm, self, serverLog.lastTerm, serverLog.lastIndex))
    startTimeoutTimer()
  }

  private def voteForMyself(): Unit = {
    voteCounter = 1
  }
}
