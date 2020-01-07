package it.unibo.sd1920.akka_raft.utils

object NodeRole {
  sealed abstract class NodeRole()
  case object SERVER extends NodeRole
  case object CLIENT extends NodeRole
}
