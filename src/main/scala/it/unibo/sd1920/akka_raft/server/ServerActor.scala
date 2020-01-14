package it.unibo.sd1920.akka_raft.server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.dispatch.ControlMessage
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.model.{BankStateMachine, CommandLog}
import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.protocol.GuiControlMessage.{GuiMsgLossServer, GuiStopServer, GuiTimeoutServer}
import it.unibo.sd1920.akka_raft.protocol.RaftMessage
import it.unibo.sd1920.akka_raft.server.ServerActor.{SchedulerTick, SchedulerTickKey}
import it.unibo.sd1920.akka_raft.utils.{NetworkConstants, RaftConstants, RandomUtil, ServerRole}
import it.unibo.sd1920.akka_raft.utils.NodeRole.NodeRole
import it.unibo.sd1920.akka_raft.utils.ServerRole.ServerRole

import scala.concurrent.duration._

private class ServerActor extends Actor with ServerActorDiscovery with LeaderBehaviour with CandidateBehaviour with FollowerBehaviour with ActorLogging with Timers {
  protected[this] val SERVERS_MAJORITY: Int = (NetworkConstants.numberOfServer / 2) + 1
  protected[this] val cluster: Cluster = Cluster(context.system)
  protected[this] var servers: Map[String, ActorRef] = Map()
  protected[this] var clients: Map[String, ActorRef] = Map()
  protected[this] var stateMachineActor: ActorRef = _
  protected[this] var currentRole: ServerRole = ServerRole.FOLLOWER
  protected[this] var currentTerm: Int = 0
  protected[this] var lastApplied: Int = 0
  protected[this] var lastCommittedIndex: Int = 0
  protected[this] var votedFor: Option[String] = None
  protected[this] val serverLog: CommandLog[BankCommand] = CommandLog.emptyLog()

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    cluster.registerOnMemberUp({
    })
    stateMachineActor = context.actorOf(BankStateMachine.props(RandomUtil.randomBetween(RaftConstants.minimumStateMachineExecutionTime,
      RaftConstants.maximumStateMachineExecutionTime).millis), "StateMachine")
  }

  override def receive: Receive = followerBehaviour

  protected def controlBehaviour: Receive = clusterDiscoveryBehaviour orElse {
    case GuiStopServer(serverID) => //TODO
    case GuiTimeoutServer(serverID) => //TODO
    case GuiMsgLossServer(serverID, loss) => //TODO
  }

  protected def startTimeoutTimer(): Unit = {
    timers startTimerWithFixedDelay(SchedulerTickKey, SchedulerTick, RandomUtil.randomBetween(RaftConstants.minimumTimeout, RaftConstants.maximumTimeout) millis)
  }

  protected def startHeartbeatTimer(): Unit = {
    timers startTimerWithFixedDelay(SchedulerTickKey, SchedulerTick, RaftConstants.heartbeatTimeout millis)
  }

  protected def broadcastMessage(raftMessage: RaftMessage): Unit = {
    servers.filter(s => s._2 != self).foreach(v => v._2 ! raftMessage)
  }

  protected def logWithRole(msg: String): Unit = log info (s"${self.path.name}:$currentRole -> $msg")
}

object ServerActor {
  //MESSAGES TO SERVER
  sealed trait ServerInput
  case class IdentifyServer(senderRole: NodeRole) extends ServerInput with ControlMessage
  case class ServerIdentity(name: String) extends ServerInput with ControlMessage
  case class ClientIdentity(name: String) extends ServerInput with ControlMessage
  //FROM STATE MACHINE
  case class StateMachineResult(result: (Int, Option[Int])) extends ServerInput
  //FROM SELF
  case object SchedulerTick extends ServerInput
  private sealed trait TimerKey
  private case object SchedulerTickKey extends TimerKey

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