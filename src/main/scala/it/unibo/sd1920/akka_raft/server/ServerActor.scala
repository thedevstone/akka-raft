package it.unibo.sd1920.akka_raft.server

import akka.actor.FSM.Timer
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, FSM, Props, Timers}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.dispatch.ControlMessage

import scala.concurrent.duration._
import scala.util.Random
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, IdentifyServer, ServerIdentity}
import it.unibo.sd1920.akka_raft.utils.{NetworkConstants, NodeRole}
import it.unibo.sd1920.akka_raft.utils.NodeRole.NodeRole
import it.unibo.sd1920.akka_raft.utils.RandomTime

class ServerActor extends Actor with ActorLogging with Timers {
  private val cluster = Cluster(context.system)
  private var servers: Map[String, ActorRef] = Map()
  private var clients: Map[String, ActorRef] = Map()

  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    cluster.registerOnMemberUp({
      timers.startTimerWithFixedDelay("","",  RandomTime.randomBetween(150,300) hours)
    })
  }

  override def receive: Receive = clusterBehaviour

  private def clusterBehaviour: Receive = {
    case MemberUp(member) => this.manageNewMember(member)
    case MemberDowned(member) =>
    case IdentifyServer(NodeRole.SERVER) => sender() ! ServerActor.ServerIdentity(self.path.name)
    case IdentifyServer(NodeRole.CLIENT) => sender() ! ClientActor.ServerIdentity(self.path.name);
    case ServerIdentity(name: String) => this.servers = this.servers + (name -> sender()); log.info(this.servers.size.toString)
    case ClientIdentity(name: String) => this.clients = this.clients + (name -> sender()); log.info(this.clients.size.toString)
  }

  private def manageNewMember(member: Member): Unit = member match {
    case m if member.roles.contains("server") =>
      context.system.actorSelection(s"${m.address}/user/**") ! ServerActor.IdentifyServer(NodeRole.SERVER)
    case m if member.roles.contains("client") =>
      context.system.actorSelection(s"${m.address}/user/**") ! ClientActor.IdentifyClient(NodeRole.SERVER)
    case _ =>
  }
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