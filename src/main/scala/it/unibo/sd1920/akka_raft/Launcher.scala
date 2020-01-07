package it.unibo.sd1920.akka_raft

import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor
import it.unibo.sd1920.akka_raft.utils.NetworkConstants

object Launcher extends App {
  ClientActor.main(Seq(NetworkConstants.firstSeedPort.toString).toArray)
  ServerActor.main(Seq(NetworkConstants.secondSeedPort.toString).toArray)
  print("Finito")
}
