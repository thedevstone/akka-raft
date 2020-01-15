package it.unibo.sd1920.akka_raft.server

import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.protocol.{AppendEntries, RequestVote, RequestVoteResult}
import it.unibo.sd1920.akka_raft.server.ServerActor.SchedulerTick
import it.unibo.sd1920.akka_raft.utils.ServerRole

private trait CandidateBehaviour {
  this: ServerActor =>

  private var voteCounter: Int = 1

  protected def candidateBehaviour: Receive = controlBehaviour orElse {
    case SchedulerTick => electionTimeout()
    case requestVote: RequestVote => handleRequestVote(requestVote)
    case requestResult: RequestVoteResult => handleVoteResult(requestResult)
    case AppendEntries(term, _, _, _) => handleAppendEntries(term)
    case _ =>
  }

  private def handleAppendEntries(term: Int): Unit = {
    if (term > currentTerm) becomingFollower(term) //TODO EasyVersion
  }

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
    currentRole = ServerRole.FOLLOWER
  }

  private def becomingLeader(): Unit = {
    logWithRole("Divento leader")
    val lastEntry: Option[Entry[BankCommand]] = serverLog.getLastEntry
    broadcastMessage(AppendEntries(currentTerm,
      if (lastEntry.isEmpty) None else serverLog.getPreviousEntry(lastEntry.get), lastEntry, serverLog.getCommitIndex))
    context.become(leaderBehaviour)
    voteForMyself()
    startHeartbeatTimer()
    leaderPreBecome()
    currentRole = ServerRole.LEADER
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
