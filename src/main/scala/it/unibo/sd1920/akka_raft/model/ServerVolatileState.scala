package it.unibo.sd1920.akka_raft.model

import it.unibo.sd1920.akka_raft.utils.ServerRole.ServerRole

case class ServerVolatileState(role: ServerRole,
                               lastApplied: (String, Int),
                               votedFor: (String, Option[String]),
                               currentTerm: (String, Int),
                               nextIndexToSend: (String, Int),
                               lastMatchedEntry: (String, Int),
                               commandLog: CommandLog[BankStateMachine.BankCommand]) {

}
