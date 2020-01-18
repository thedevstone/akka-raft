package it.unibo.sd1920.akka_raft.protocol

import akka.actor.ActorRef
import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.model.Entry

sealed trait RaftMessage

/**
 * Request Vote is used by candidates to request votes from followers.
 *
 * @param candidateTerm the candidate term
 * @param candidateId   the candidate id
 * @param lastLogTerm   the candidate last log term
 * @param lastLogIndex  the candidate last log index
 */
case class RequestVote(
  candidateTerm: Int,
  candidateId: ActorRef,
  lastLogTerm: Int,
  lastLogIndex: Int
) extends RaftMessage {
  assert(candidateTerm >= 0)
  assert(lastLogTerm >= 0)
}

/**
 * Message sent from receiver to sender. It is similar to an ack
 *
 * @param voteGranted  if vote is granted
 * @param followerTerm the follower term
 */
case class RequestVoteResult(
  voteGranted: Boolean,
  followerTerm: Int
) {
  assert(followerTerm >= 0)
}

/**
 * Append entries message sent by leader.
 * <p>
 * It is used as Heartbeat if entry is empty, otherwise it is used to replicate the leader log.
 *
 * @param leaderTerm       the leader term
 * @param previousEntry    the previous entry to check
 * @param entry            the entry to append
 * @param leaderLastCommit the leader last commit index
 */
case class AppendEntries(
  leaderTerm: Int,
  previousEntry: Option[Entry[BankCommand]],
  entry: Option[Entry[BankCommand]],
  leaderLastCommit: Int,
) extends RaftMessage {
  assert(leaderTerm >= 0)
  assert(leaderLastCommit >= -1)
}

/**
 * Message sent from follower to leader. It is similar to an ack
 *
 * @param success    if the AppendEntries message succeeded
 * @param matchIndex the last index that matches in leader log and follower log
 * @param term       the follower term
 */
case class AppendEntriesResult(
  success: Boolean,
  matchIndex: Int,
  term: Int
) extends RaftMessage

/**
 * Message used by follower to redirect client to leader.
 *
 * @param requestID the request id present in client cache
 * @param leaderRef the leader reference
 */
case class Redirect(
  requestID: Int,
  leaderRef: Option[ActorRef]
) extends RaftMessage

/**
 * Client request message sent from client to server leader.
 *
 * @param requestID the request id present in client cache
 * @param command   the client request command
 */
case class ClientRequest(requestID: Int, command: BankCommand) extends RaftMessage

/**
 * Client request result message sent from server leader to client.
 *
 * @param requestID the request id
 * @param success   if the request succeeded
 * @param result    the request result form the state machines
 */
case class RequestResult(requestID: Int, success: Boolean, result: Option[Int]) extends RaftMessage
