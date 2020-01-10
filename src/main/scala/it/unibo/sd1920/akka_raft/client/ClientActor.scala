package it.unibo.sd1920.akka_raft.client

import akka.actor.TypedActor.Receiver
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.dispatch.ControlMessage
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.client.ClientActor.{GuiCommand, Result}
import it.unibo.sd1920.akka_raft.server.ServerActor
import it.unibo.sd1920.akka_raft.utils.NetworkConstants
import it.unibo.sd1920.akka_raft.utils.NodeRole.NodeRole

private class ClientActor extends Actor with ClientActorDiscovery with ActorLogging {
  protected[this] val cluster: Cluster = Cluster(context.system)
  protected[this] var servers: Map[String, ActorRef] = Map()
  protected[this] var clients: Map[String, ActorRef] = Map()
  private var requestID:Int = 0


  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    cluster.registerOnMemberUp({})
  }

  override def receive: Receive = clusterBehaviour orElse onMessage

  private def onMessage: Receive = {
    case GuiCommand(1) => this.servers.get(???).get ! ServerActor.ClientRequest(requestID,"")
    case Result => this.requestID += 1
    case GuiCommand(3) =>
    case GuiCommand(4) =>
  }


}

object ClientActor {
  //MESSAGES TO CLIENT
  sealed trait ClientInput
  case class IdentifyClient(senderRole: NodeRole) extends ClientInput with ControlMessage
  case class ServerIdentity(name: String) extends ClientInput with ControlMessage
  case class ClientIdentity(name: String) extends ClientInput with ControlMessage
  case class Result(name: String) extends ClientInput with ControlMessage

  sealed trait GuiClientMessage extends ClientInput
  case class GuiCommand(commandType:Int) extends GuiClientMessage with ControlMessage


  //STARTING CLIENT
  def props: Props = Props(new ClientActor())

  def main(args: Array[String]): Unit = {
    val name = args(0)
    val port = if (args.length == 1) "0" else args(1)

    val config = ConfigFactory.parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load("client"))
    val system = ActorSystem(NetworkConstants.clusterName, config)
    system actorOf(ClientActor.props, name)
  }
}