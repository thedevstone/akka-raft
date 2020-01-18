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
  val minimumTimeout = 1250000
  val maximumTimeout = 1500000
  val minimumStateMachineExecutionTime = 1000
  val maximumStateMachineExecutionTime = 2000
  val heartbeatTimeout = 700000
}
