package it.unibo.sd1920.akka_raft.utils

object NetworkConstants {
  val numberOfServer = 5
  val firstSeedPort = 5000
  val secondSeedPort = 5001
  val systemAddress = "127.0.0.1"
  val clusterName = "RaftCluster"
}

object RaftConstants {
  val minimumTimeout = 150
  val maximumTimeout = 300
  val minimumStateMachineExecutionTime = 500
  val maximumStateMachineExecutionTime = 2000
  val heartbeatTimeout = 100
}
