package it.unibo.sd1920.akka_raft.client

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.utils.NetworkConstants

class ClientActor extends Actor {
  override def receive: Receive = {
    case _ =>
  }
}

object ClientActor {
  def props: Props = Props(new ClientActor())

  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load("client"))
    val system = ActorSystem(NetworkConstants.clusterName, config)
    system actorOf (ClientActor.props)
  }
}