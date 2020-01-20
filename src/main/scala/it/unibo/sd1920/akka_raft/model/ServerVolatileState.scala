package it.unibo.sd1920.akka_raft.model

import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.utils.ServerRole.ServerRole

/**
 * Represents the state of the server, it is used to update server state in gui.
 *
 * @param role             the server role
 * @param lastCommitted    the server last committed index
 * @param lastApplied      the server last applied index
 * @param votedFor         id of the voted server
 * @param currentTerm      the server current term
 * @param nextIndexToSend  the server next index to send
 * @param lastMatchedIndex the server last match index
 * @param commandLog       the server replicated log
 */
case class ServerVolatileState(
  role: ServerRole,
  lastCommitted: Int,
  lastApplied: Int,
  votedFor: Option[String],
  currentTerm: Int,
  nextIndexToSend: Int,
  lastMatchedIndex: Int,
  commandLog: List[Entry[BankCommand]])
