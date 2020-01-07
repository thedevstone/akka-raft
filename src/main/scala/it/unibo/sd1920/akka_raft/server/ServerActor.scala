package it.unibo.sd1920.akka_raft.server

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.utils.NetworkConstants

class ServerActor extends Actor with ActorLogging {
  private val cluster = Cluster(context.system)
  log debug ("ciao")
  override def receive: Receive = {
    case _ =>
  }
}

object ServerActor {
  def props: Props = Props(new ServerActor())

  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load("server"))
    val system = ActorSystem(NetworkConstants.clusterName, config)
    system actorOf (ServerActor.props)
  }
}