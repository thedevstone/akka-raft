package it.unibo.sd1920.akka_raft.client

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.dispatch.ControlMessage
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.model.{BankStateMachine, ServerVolatileState}
import it.unibo.sd1920.akka_raft.model.BankStateMachine._
import it.unibo.sd1920.akka_raft.protocol.{ClientRequest, Redirect, RequestResult}
import it.unibo.sd1920.akka_raft.protocol.GuiControlMessage._
import it.unibo.sd1920.akka_raft.utils.{CommandType, NetworkConstants}
import it.unibo.sd1920.akka_raft.utils.CommandType.CommandType
import it.unibo.sd1920.akka_raft.utils.NodeRole.NodeRole
import it.unibo.sd1920.akka_raft.view.screens.{ClientObserver, MainScreenView}

import scala.collection.immutable.ListMap
import scala.util.Random

private class ClientActor extends Actor with ClientActorDiscovery with ActorLogging {
  protected[this] val view: ClientObserver = MainScreenView()
  protected[this] val cluster: Cluster = Cluster(context.system)
  protected[this] var servers: Map[String, ActorRef] = Map()
  protected[this] var clients: Map[String, ActorRef] = Map()
  protected[this] var requestHistory: Map[Int, ResultState] = ListMap()
  protected[this] var requestID: Int = 0

  view.setViewActorRef(self)

  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    cluster.registerOnMemberUp({})
  }

  override def receive: Receive = clusterBehaviour

  def onMessage: Receive = {
    //RAFT
    case Redirect(reqID, leaderRef) => handleRedirect(reqID, leaderRef)
    //TODO Timer
    //FROM SERVER TO GUI
    case result: RequestResult => handleResult(result)
    case GuiServerState(serverState) => guiUpdateServerInfo(serverState)
    //FROM GUI TO SERVER
    case GuiStopServer(serverID) => servers(serverID) ! GuiStopServer(serverID)
    case GuiTimeoutServer(serverID) => servers(serverID) ! GuiTimeoutServer(serverID)
    case GuiMsgLossServer(serverID, loss) => servers(serverID) ! GuiMsgLossServer(serverID, loss)
    case GuiSendMessage(serverID, commandType, iban, amount) => elaborateGuiSendRequest(serverID, commandType, iban, amount)
    case Log(message) => log info message
    case UpdateGui() => view.updateResultState(requestHistory)
    case RetryMessage(indexInMap: Int, serverID: String) => handleRetryMessage(indexInMap, serverID)
  }

  //RAFT
  def handleRedirect(reqID: Int, leaderRef: Option[ActorRef]): Unit = {
    val request = requestHistory(reqID)
    val targetServer: ActorRef = leaderRef match {
      case None => servers.values.toList(Random.nextInt(servers.size))
      case Some(ref: ActorRef) => ref
    }
    targetServer ! ClientRequest(reqID, request.command)
  }

  //FROM SERVER TO GUI
  private def guiUpdateServerInfo(serverVolatileState: ServerVolatileState): Unit = {
    val nodeId = resolveNodeID(sender())
    view.updateServerState(nodeId, serverVolatileState)
  }

  private def handleResult(result: RequestResult): Unit = {
    this.requestHistory = this.requestHistory + (result.id -> ResultState(executed = true, requestHistory(result.id).command, Some(result)))
    view.updateResultState(requestHistory)
  }

  //FROM GUI TO SERVER
  private def elaborateGuiSendRequest(targetServer: String, command: CommandType, iban: String, amount: String): Unit = {
    var amountInt = 0
    try amountInt = amount.toInt catch {
      case _: Throwable =>
    }
    val serverCommand = command match {
      case CommandType.DEPOSIT => BankStateMachine.Deposit(iban, amountInt)
      case CommandType.WITHDRAW => BankStateMachine.Withdraw(iban, amountInt)
      case CommandType.GET_BALANCE => BankStateMachine.GetBalance(iban)
    }
    this.requestHistory = this.requestHistory + (this.requestID -> ResultState(executed = false, serverCommand, None))
    servers(targetServer) ! ClientRequest(requestID, serverCommand)
    this.requestID += 1
  }

  def handleRetryMessage(indexInMap: Int, serverID: String): Unit = {
    val requestEntry = requestHistory.toList(indexInMap)
    this.requestHistory = this.requestHistory + (requestEntry._1 -> ResultState(executed = false, requestEntry._2.command, None))
    servers(serverID) ! ClientRequest(requestEntry._1, requestEntry._2.command)
  }

  private def resolveNodeID(actorRef: ActorRef): String = servers.filter(e => e._2 == sender()).last._1
}

object ClientActor {

  //MESSAGES TO CLIENT
  sealed trait ClientInput
  case class IdentifyClient(senderRole: NodeRole) extends ClientInput with ControlMessage
  case class ServerIdentity(name: String) extends ClientInput with ControlMessage
  case class ClientIdentity(name: String) extends ClientInput with ControlMessage


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

case class ResultState(
  executed: Boolean,
  command: BankCommand,
  result: Option[RequestResult]
)
