package it.unibo.sd1920.akka_raft.utils

/**
 * Network constants for Akka Cluster.
 */
object NetworkConstants {
  val numberOfServer = 5
  val firstSeedPort = 5000
  val secondSeedPort = 5001
  val systemAddress = "127.0.0.1"
  val clusterName = "RaftCluster"
}

/**
 * Raft constants.
 */
object RaftConstants {
  val minimumTimeout = 2000
  val maximumTimeout = 3500
  val minimumStateMachineExecutionTime = 1000
  val maximumStateMachineExecutionTime = 2000
  val heartbeatTimeout = 250

}
