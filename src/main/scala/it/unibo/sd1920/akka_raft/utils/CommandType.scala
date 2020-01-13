package it.unibo.sd1920.akka_raft.utils

object CommandType extends Enumeration {
  type CommandType = Value
  val DEPOSIT, WITHDRAW, GET_BALANCE = Value
}