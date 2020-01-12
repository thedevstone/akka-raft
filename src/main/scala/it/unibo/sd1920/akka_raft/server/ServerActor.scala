package it.unibo.sd1920.akka_raft.server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.dispatch.ControlMessage
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.model.{BankStateMachine, CommandLog}
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientRequest, GuiCommand, SchedulerTick, SchedulerTickKey}
import it.unibo.sd1920.akka_raft.utils.NetworkConstants
import it.unibo.sd1920.akka_raft.utils.NodeRole.NodeRole
import it.unibo.sd1920.akka_raft.utils.RandomUtil
import it.unibo.sd1920.akka_raft.utils
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._


private class ServerActor extends Actor with ServerActorDiscovery with LeaderBehaviour with CandidateBehaviour with FollowerBehaviour with ActorLogging with Timers {
  protected[this] val cluster: Cluster = Cluster(context.system)
  protected[this] var servers: Map[String, ActorRef] = Map()
  protected[this] var clients: Map[String, ActorRef] = Map()

  protected[this] var currentTerm: Int = 0
  protected[this] var lastApplied: Int = 0
  protected[this] var lastCommittedIndex: Int = 0
  protected[this] var votedFor: Option[String] = None
  protected[this] val serverLog: CommandLog[BankCommand] = CommandLog.emptyLog()



  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    cluster.registerOnMemberUp({
    })
    context.actorOf(BankStateMachine.props(RandomUtil.randomBetween(500,2000).millis),"StateMachine")
  }

  override def receive: Receive = followerBehaviour

  private def onMessage: Receive = clusterBehaviour orElse {
    case ClientRequest(requestID,bankCommand) =>
    case GuiCommand(2) => this.context.become(leaderBehaviour,true)
    case GuiCommand(3) =>
    case GuiCommand(4) =>
  }

  protected def startTimer(){
    timers startTimerWithFixedDelay(SchedulerTickKey,SchedulerTick,RandomUtil.randomBetween(150,300) millis)
  }


}

object ServerActor {
  //MESSAGES TO SERVER
  sealed trait ServerInput
  case class IdentifyServer(senderRole: NodeRole) extends ServerInput with ControlMessage
  case class ServerIdentity(name: String) extends ServerInput with ControlMessage
  case class ClientIdentity(name: String) extends ServerInput with ControlMessage
  case class ClientRequest(requestID: Int,command: BankCommand) extends ServerInput
  case object SchedulerTick extends ServerInput

  sealed trait GuiServerMessage extends ServerInput
  case class GuiCommand(commandType:Int) extends GuiServerMessage

  private sealed trait TimerKey
  private case object SchedulerTickKey extends TimerKey

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