package it.unibo.sd1920.akka_raft.server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.utils.NetworkConstants
import it.unibo.sd1920.akka_raft.utils.NodeRole.NodeRole

private class ServerActor extends Actor with ServerActorDiscovery with ActorLogging with Timers {
  protected[this] val cluster: Cluster = Cluster(context.system)
  protected[this] var servers: Map[String, ActorRef] = Map()
  protected[this] var clients: Map[String, ActorRef] = Map()

  override def receive: Receive = clusterBehaviour
}

object ServerActor {
  //MESSAGES TO SERVER
  sealed trait ServerInput
  case class IdentifyServer(senderRole: NodeRole) extends ServerInput
  case class ServerIdentity(name: String)
  case class ClientIdentity(name: String)

  //STARTING CLIENT
  def props: Props = Props(new ServerActor())

  def main(args: Array[String]): Unit = {
    val name = args(0)
    val port = if (args.length == 1) "0" else args(1)

    val config = ConfigFactory.parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load("server"))
    val system = ActorSystem(NetworkConstants.clusterName, config)
    system actorOf(ServerActor.props, name)
  }
}