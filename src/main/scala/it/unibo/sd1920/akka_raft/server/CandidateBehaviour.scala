package it.unibo.sd1920.akka_raft.server

import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.protocol.{AppendEntries, RequestVote, RequestVoteResult}
import it.unibo.sd1920.akka_raft.server.ServerActor.SchedulerTick
import it.unibo.sd1920.akka_raft.utils.ServerRole

private trait CandidateBehaviour {
  this: ServerActor =>

  private var voteCounter: Int = 1

  protected def candidateBehaviour: Receive = controlBehaviour orElse MessageInterceptor({
    case SchedulerTick => electionTimeout()
    case requestVote: RequestVote => handleRequestVote(requestVote)
    case requestResult: RequestVoteResult => handleVoteResult(requestResult)
    case AppendEntries(term, _, _, _) => checkBehindTerm(term)
    case _ =>
  })

  //EVENTS
  /**
   * Handles Timeout event.
   * <p>
   * When a timeout triggers then a '''new election''' is started. Term is increased, candidate vote for itself and it broadcast
   * RequestVote messages. Then timeout is restarted.
   *
   */
  private def electionTimeout(): Unit = {
    currentTerm += 1
    voteForMyself()
    broadcastMessage(RequestVote(currentTerm, self, serverLog.lastTerm, serverLog.lastIndex))
    startTimeoutTimer()
  }

  //REQUEST VOTES FROM CANDIDATES
  /**
   * Handles RequestVote message.
   * <p>
   * When a request vote arrives to candidate then it has to deny the request or in some special case accept convert and vote.
   *
   * @param requestVote the request vote
   */
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

  /**
   * Handles RequestVoteResult message.
   * <p>
   * When a vote result arrives to candidate then it has to:
   *    - become follower if candidate term is outdated
   *    - become leader if candidate has the majority of correct votes
   *
   * @param result the result of the request
   */
  private def handleVoteResult(result: RequestVoteResult): Unit = result match {
    case RequestVoteResult(_, followerTerm) if followerTerm > currentTerm => becomingFollower(followerTerm)
    case RequestVoteResult(result, followerTerm) if result && followerTerm == currentTerm =>
      voteCounter += 1
      if (voteCounter >= SERVERS_MAJORITY) becomingLeader()
    case _ =>
  }

  /**
   * Vote for candidate.
   *
   * @param term the candidate term
   */
  private def voteForApplicantCandidate(term: Int) {
    becomingFollower(term)
    votedFor = Some(sender().path.name)
    sender() ! RequestVoteResult(voteGranted = true, currentTerm)
  }

  /**
   * Become follower.
   *
   * @param term the candidate term
   */
  private def becomingFollower(term: Int) {
    currentTerm = term
    context.become(followerBehaviour)
    voteForMyself()
    startTimeoutTimer()
    currentRole = ServerRole.FOLLOWER
  }

  private def becomingLeader(): Unit = {
    logWithRole(s"Becoming leader")
    context.become(leaderBehaviour)
    voteForMyself()
    startHeartbeatTimer()
    followerStateInitialization()
    currentRole = ServerRole.LEADER
    val lastEntry: Option[Entry[BankCommand]] = serverLog.getLastEntry
    broadcastMessage(AppendEntries(currentTerm,
      if (lastEntry.isEmpty) None else serverLog.getPreviousEntry(lastEntry.get), lastEntry, serverLog.getCommitIndex))
  }

  /**
   * Check if the candidate is behind an other server.
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

  /**
   * Reset the vote counter and
   */
  private def voteForMyself(): Unit = {
    voteCounter = 1
  }
}
