package it.unibo.sd1920.akka_raft.utils

/**
 * Server node role used in node discovery in Akka Clustering.
 */
object NodeRole {
  sealed abstract class NodeRole()
  case object SERVER extends NodeRole
  case object CLIENT extends NodeRole
}
