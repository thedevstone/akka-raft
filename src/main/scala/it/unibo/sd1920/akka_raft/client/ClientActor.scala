package it.unibo.sd1920.akka_raft.client

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.client.ClientActor.{ClientIdentity, IdentifyClient, ServerIdentity}
import it.unibo.sd1920.akka_raft.server.ServerActor
import it.unibo.sd1920.akka_raft.utils.{NetworkConstants, NodeRole}
import it.unibo.sd1920.akka_raft.utils.NodeRole.NodeRole

class ClientActor extends Actor with ActorLogging {
  private val cluster = Cluster(context.system)
  private var servers: Map[String, ActorRef] = Map()
  private var clients: Map[String, ActorRef] = Map()

  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    cluster.registerOnMemberUp({})
  }

  override def receive: Receive = clusterBehaviour

  private def clusterBehaviour: Receive = {
    case MemberUp(member) => this.manageNewMember(member)
    case MemberDowned(member) =>
    case IdentifyClient(NodeRole.CLIENT) => sender() ! ClientActor.ClientIdentity(self.path.name)
    case IdentifyClient(NodeRole.SERVER) => sender() ! ServerActor.ClientIdentity(self.path.name)
    case ClientIdentity(name: String) => this.clients = this.clients + (name -> sender())
    case ServerIdentity(name: String) => this.servers = this.servers + (name -> sender())
  }

  private def manageNewMember(member: Member): Unit = member match {
    case m if member.roles.contains("server") =>
      context.system.actorSelection(s"${m.address}/user/**") ! ServerActor.IdentifyServer(NodeRole.CLIENT);
    case m if member.roles.contains("client") =>
      context.system.actorSelection(s"${m.address}/user/**") ! ClientActor.IdentifyClient(NodeRole.CLIENT);
    case _ =>
  }
}

object ClientActor {
  //MESSAGES TO CLIENT
  sealed trait ClientInput
  case class IdentifyClient(senderRole: NodeRole) extends ClientInput
  case class ServerIdentity(name: String)
  case class ClientIdentity(name: String)

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