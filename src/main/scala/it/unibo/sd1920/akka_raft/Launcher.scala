package it.unibo.sd1920.akka_raft

import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor
import it.unibo.sd1920.akka_raft.utils.NetworkConstants

object Launcher extends App {
  //SEEDS
  ClientActor.main(Seq("C0", NetworkConstants.firstSeedPort.toString).toArray)
  ServerActor.main(Seq("S0", NetworkConstants.secondSeedPort.toString).toArray)
  //SERVERS
  ServerActor.main(Seq("S1").toArray)
  ServerActor.main(Seq("S2").toArray)
  ServerActor.main(Seq("S3").toArray)
  ServerActor.main(Seq("S4").toArray)
}
