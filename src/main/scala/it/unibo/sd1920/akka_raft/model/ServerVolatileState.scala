package it.unibo.sd1920.akka_raft.model

import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.utils.ServerRole.ServerRole

case class ServerVolatileState(role: ServerRole,
                               lastCommitted: Int,
                               lastApplied: Int,
                               votedFor: Option[String],
                               currentTerm: Int,
                               nextIndexToSend: Int,
                               lastMatchedEntry: Int,
                               commandLog: List[Entry[BankCommand]]) {

}
