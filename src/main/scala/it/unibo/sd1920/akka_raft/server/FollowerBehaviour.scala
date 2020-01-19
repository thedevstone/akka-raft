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
    checkAndUpdateTerm(appendEntry.leaderTerm)
    appendEntry match {
      // ############# SPECIAL CASES
      //reject message from leader behind me
      case AppendEntries(leaderTerm, _, _, _) if leaderTerm < currentTerm =>
        lastMatched = -1
        sender() ! AppendEntriesResult(success = false, lastMatched, currentTerm)
      //leader has empty log and send empty previous entry and empty entry. So empty heartbeat
      case AppendEntries(_, previousEntry, entry, _) if previousEntry.isEmpty && entry.isEmpty =>
        lastMatched = -1
        sender() ! AppendEntriesResult(success = true, lastMatched, currentTerm)
      //leader send first entry presents in log, previous is empty. Insert entry and handle commit
      case AppendEntries(_, previousEntry, entry, leaderLastCommit) if previousEntry.isEmpty && entry.nonEmpty =>
        val result: Boolean = serverLog.insertEntry(entry.get)
        handleCommit(leaderLastCommit)
        lastMatched = 0
        sender() ! AppendEntriesResult(success = result, lastMatched, currentTerm)
      // ############# NORMAL OPERATIONS
      //consistency check totally fails. If follower does not contains previous entry then all log diverges
      case AppendEntries(_, previousEntry, _, _) if !serverLog.contains(previousEntry.get) =>
        lastMatched = -1
        sender() ! AppendEntriesResult(success = false, lastMatched, currentTerm)
      //consistency check works and previous entry is present. insert entry and handle commit.
      case AppendEntries(_, _, entry, leaderLastCommit) if entry.nonEmpty =>
        val result: Boolean = serverLog.insertEntry(entry.get)
        handleCommit(leaderLastCommit)
        lastMatched = entry.get.index
        sender() ! AppendEntriesResult(success = result, lastMatched, currentTerm)
      //if simple heartbeat return true and perform commit
      case AppendEntries(_, previousEntry, _, leaderLastCommit) =>
        handleCommit(leaderLastCommit)
        lastMatched = previousEntry.get.index
        sender() ! AppendEntriesResult(success = true, lastMatched, currentTerm)
      case _ =>
    }
    startTimeoutTimer()
  }

  private def handleCommit(leaderLastCommit: Int): Unit = {
    if (leaderLastCommit > serverLog.getCommitIndex) callCommit(Math.min(serverLog.lastIndex, leaderLastCommit))
  }
}
