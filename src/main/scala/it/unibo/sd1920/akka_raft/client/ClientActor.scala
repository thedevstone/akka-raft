package it.unibo.sd1920.akka_raft.client

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.dispatch.ControlMessage
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.client.ClientActor.{GuiCommand, ResultArrived}
import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.server.ServerActor
import it.unibo.sd1920.akka_raft.utils.NetworkConstants
import it.unibo.sd1920.akka_raft.utils.NodeRole.NodeRole
import it.unibo.sd1920.akka_raft.view.screens.{ClientObserver, MainScreenView}

private class ClientActor extends Actor with ClientActorDiscovery with ActorLogging {
  protected[this] val cluster: Cluster = Cluster(context.system)
  protected[this] val view: ClientObserver = MainScreenView()
  view.setViewActorRef(self)

  protected[this] var servers: Map[String, ActorRef] = Map()
  protected[this] var clients: Map[String, ActorRef] = Map()
  private var requestHistory: Map[Int, Result] = Map()
  private var requestID: Int = 0


  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    cluster.registerOnMemberUp({})
  }

  override def receive: Receive = clusterBehaviour orElse onMessage

  def onMessage: Receive = {
    case GuiCommand(targetServer, command) => elaborateGuiRequest(targetServer, command)
    case ResultArrived(id, result) => handleResult(id, result)
  }

  private def elaborateGuiRequest(targetServer: String, command: BankCommand): Unit = {
    sendRequest(targetServer, command)
    this.requestHistory = Map(this.requestID -> Result(false, command, None))
    this.requestID += 1
  }

  private def handleResult(reqID: Int, result: Option[Int]): Unit = {
    this.requestHistory = Map(reqID -> Result(true, this.requestHistory(reqID).command, result))
  }

  private def sendRequest(targetServer: String, command: BankCommand): Unit = {
    this.servers(targetServer) ! ServerActor.ClientRequest(requestID, command);
  }

}

object ClientActor {
  //MESSAGES TO CLIENT
  sealed trait ClientInput
  case class IdentifyClient(senderRole: NodeRole) extends ClientInput with ControlMessage
  case class ServerIdentity(name: String) extends ClientInput with ControlMessage
  case class ClientIdentity(name: String) extends ClientInput with ControlMessage
  case class ResultArrived(id: Int, result: Option[Int]) extends ClientInput

  sealed trait GuiClientMessage extends ClientInput
  case class GuiCommand(targetServer: String, command: BankCommand) extends GuiClientMessage with ControlMessage
  case class Log(message: String) extends GuiClientMessage with ControlMessage

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


case class Result(
                   executed: Boolean,
                   command: BankCommand,
                   result: Option[Int]
                 )
