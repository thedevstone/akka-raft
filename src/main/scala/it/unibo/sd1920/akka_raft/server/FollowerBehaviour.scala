package it.unibo.sd1920.akka_raft.server


import akka.actor.ActorRef
import it.unibo.sd1920.akka_raft.protocol._
import it.unibo.sd1920.akka_raft.server.ServerActor.SchedulerTick
import it.unibo.sd1920.akka_raft.utils.ServerRole

private trait FollowerBehaviour {
  this: ServerActor =>

  private var leaderRef: Option[ActorRef] = None

  protected def followerBehaviour: Receive = controlBehaviour orElse MessageInterceptor({
    case SchedulerTick => followerTimeout()
    case requestVote: RequestVote => handleRequestVote(requestVote)
    case appendEntry: AppendEntries => handleAppendEntries(appendEntry)
    case ClientRequest(requestID, _) => sender() ! Redirect(requestID, leaderRef)
    case _ =>
  })

  //EVENTS
  /**
   * Handles Timeout event.
   * <p>
   * When a timeout triggers then the follower become '''candidate'''. Increments the term by 1 and broadcast RequestVote messages.
   *
   */
  private def followerTimeout(): Unit = {
    logWithRole(s"Becoming Candidate")
    leaderRef = None
    currentTerm += 1
    context.become(candidateBehaviour)
    broadcastMessage(RequestVote(currentTerm, self, serverLog.lastTerm, serverLog.lastIndex))
    startTimeoutTimer()
    currentRole = ServerRole.CANDIDATE
  }

  //APPEND ENTRIES FROM LEADER
  /**
   * Handles AppendEntries message
   * <p>
   * Follower has to send different responses based on AppendEntries message:
   *    - Send negative response if leader term is not updated as follower term
   * -
   *
   * @param appendEntry AppendEntries message
   */
  private def handleAppendEntries(appendEntry: AppendEntries): Unit = {
    leaderRef = Some(sender())
    var success: Boolean = false
    var lastMatchedIndex: Int = -1
    checkAndUpdateTerm(appendEntry.leaderTerm)
    appendEntry match {
      //reject message from leader behind me
      case AppendEntries(leaderTerm, _, _, _) if leaderTerm < currentTerm =>
        lastMatchedIndex = -1
        success = false
      //leader has empty log and send empty previous entry and empty entry. So empty heartbeat
      case AppendEntries(_, previousEntry, entry, _) if previousEntry.isEmpty && entry.isEmpty =>
        lastMatchedIndex = -1
        success = true
      //leader send first entry presents in log, previous is empty. Insert entry and handle commit
      case AppendEntries(_, previousEntry, entry, leaderLastCommit) if previousEntry.isEmpty && entry.nonEmpty =>
        success = serverLog.insertEntry(entry.get)
        handleCommit(leaderLastCommit)
        lastMatchedIndex = 0
      //consistency check totally fails. If follower does not contains previous entry then all log diverges
      case AppendEntries(_, previousEntry, _, _) if !serverLog.contains(previousEntry.get) =>
        lastMatchedIndex = -1
        success = false
      //consistency check works and previous entry is present. insert entry and handle commit.
      case AppendEntries(_, _, entry, leaderLastCommit) if entry.nonEmpty =>
        success = serverLog.insertEntry(entry.get)
        handleCommit(leaderLastCommit)
        lastMatchedIndex = entry.get.index
      //if simple heartbeat return true and perform commit
      case AppendEntries(_, previousEntry, _, leaderLastCommit) =>
        handleCommit(leaderLastCommit)
        lastMatchedIndex = previousEntry.get.index
        success = true
      case _ =>
    }
    lastMatched = lastMatchedIndex
    sender() ! AppendEntriesResult(success, lastMatchedIndex, currentTerm)
    startTimeoutTimer()
  }

  /**
   * Check if candidate term is more updated than follower term, if so update follower term.
   *
   * @param term candidate term
   */
  private def checkAndUpdateTerm(term: Int): Unit = {
    if (term > currentTerm) {
      currentTerm = term
      votedFor = None
    }
  }

  private def handleCommit(leaderLastCommit: Int): Unit = {
    if (leaderLastCommit > serverLog.getCommitIndex) callCommit(Math.min(serverLog.lastIndex, leaderLastCommit))
  }
}
