package it.unibo.sd1920.akka_raft.server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.dispatch.ControlMessage
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.model.{BankStateMachine, CommandLog, ServerVolatileState}
import it.unibo.sd1920.akka_raft.model.Bank.BankTransactionResult
import it.unibo.sd1920.akka_raft.model.BankStateMachine.{ApplyCommand, BankCommand}
import it.unibo.sd1920.akka_raft.protocol.{RaftMessage, RequestVote, RequestVoteResult}
import it.unibo.sd1920.akka_raft.protocol.GuiControlMessage._
import it.unibo.sd1920.akka_raft.server.ServerActor.{InternalMessage, SchedulerTick, SchedulerTickKey}
import it.unibo.sd1920.akka_raft.utils.{NetworkConstants, RaftConstants, RandomUtil, ServerRole}
import it.unibo.sd1920.akka_raft.utils.NodeRole.NodeRole
import it.unibo.sd1920.akka_raft.utils.ServerRole.ServerRole

import scala.concurrent.duration._
import scala.util.Random

/**
 * Server node of the Akka cluster.
 */
private class ServerActor extends Actor with ServerActorDiscovery with LeaderBehaviour with CandidateBehaviour with FollowerBehaviour with ActorLogging with Timers {
  //CLUSTER NODE
  protected[this] val HALF_FOLLOWERS_NUMBER: Int = NetworkConstants.numberOfServer / 2
  protected[this] val cluster: Cluster = Cluster(context.system)
  protected[this] var servers: Map[String, ActorRef] = Map()
  protected[this] var clients: Map[String, ActorRef] = Map()
  //RAFT STATE
  protected[this] val serverLog: CommandLog[BankCommand] = CommandLog.emptyLog()
  protected[this] var stateMachineActor: ActorRef = _
  protected[this] var lastApplied: Int = 0
  protected[this] var currentRole: ServerRole = ServerRole.FOLLOWER
  protected[this] var currentTerm: Int = 0
  protected[this] var votedFor: Option[String] = None
  //CONTROL STATE
  protected[this] var lastMatched: Int = 0
  protected[this] var stopped: Boolean = false
  protected[this] var messageLossThreshold: Double = 1.0

  /**
   * Subscribe to cluster and start the state machine.
   */
  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    cluster.registerOnMemberUp({
    })
    stateMachineActor = context.actorOf(BankStateMachine.props(RandomUtil.randomBetween(RaftConstants.minimumStateMachineExecutionTime,
      RaftConstants.maximumStateMachineExecutionTime).millis), "StateMachine")
  }


  /**
   * Intercept all messages and decide if drop some of them. Dropping depends on gui commands.
   *
   * @param receiver the receiver
   */
  case class MessageInterceptor(receiver: Receive) extends Receive {
    def apply(msg: Any): Unit = {
      //clients.last._2 ! GuiServerState(ServerVolatileState(currentRole, serverLog.getCommitIndex, lastApplied, votedFor, currentTerm, serverLog.nextIndex, lastMatched, serverLog.getEntries)) //TODO da cancellare
      receiver.apply(msg)
      clients.last._2 ! GuiServerState(ServerVolatileState(currentRole, serverLog.getCommitIndex, lastApplied, votedFor, currentTerm, serverLog.nextIndex, lastMatched, serverLog.getEntries)) //TODO da cancellare
    }
    def isDefinedAt(msg: Any): Boolean = {
      if ((stopped || Random.nextDouble() > messageLossThreshold && (!classOf[InternalMessage].isAssignableFrom(msg.getClass))) && (!classOf[ControlMessage].isAssignableFrom(msg.getClass))) {
        logWithRole("Messaggio bloccato:: " + msg.toString
        )
        return false
      }
      receiver.isDefinedAt(msg)
    }
  }

  override def receive: Receive = clusterDiscoveryBehaviour

  /**
   * Behaviour for handling gui control messages.
   *
   * @return the behaviour
   */
  protected def controlBehaviour: Receive = clusterDiscoveryBehaviour orElse {
    case GuiStopServer(_) => stopped = true
      logWithRole("\n\t\tStopped:")
    case GuiResumeServer(_) => stopped = false
      logWithRole("\n\t\tReasume:")
    case GuiTimeoutServer(_) => handleRequestTimeOut()
    case GuiMsgLossServer(_, loss) => logWithRole("\n\t\tvalore:" + loss)
      messageLossThreshold = loss
  }

  /**
   * Handles GuiTimeoutServer.
   * <p>
   * Send a timeout message that triggers timeout and restart timers.
   */
  private def handleRequestTimeOut() {
    self ! SchedulerTick
    currentRole match {
      case ServerRole.FOLLOWER => startTimeoutTimer()
      case ServerRole.CANDIDATE => startTimeoutTimer()
      case ServerRole.LEADER => startHeartbeatTimer()
    }
  }

  /**
   * Start the timeout timer.
   */
  protected def startTimeoutTimer(): Unit = {
    timers startTimerWithFixedDelay(SchedulerTickKey, SchedulerTick, RandomUtil.randomBetween(RaftConstants.minimumTimeout, RaftConstants.maximumTimeout) millis)
  }

  /**
   * Start the heartbeat timer.
   */
  protected def startHeartbeatTimer(): Unit = {
    timers startTimerWithFixedDelay(SchedulerTickKey, SchedulerTick, RaftConstants.heartbeatTimeout millis)
  }

  /**
   * Broadcast a raft message to all servers.
   *
   * @param raftMessage the message
   */
  protected def broadcastMessage(raftMessage: RaftMessage): Unit = {
    servers.filter(s => s._2 != self).foreach(v => v._2 ! raftMessage)
  }

  /**
   * Commit all the entries up to index. Then apply all committed entries to state machine.
   *
   * @param index the commit index
   */
  protected def callCommit(index: Int): Unit = {
    val lastCommitted: Int = serverLog.getCommitIndex
    serverLog.commit(index)
    serverLog.getEntriesBetween(lastCommitted, index).foreach(e => stateMachineActor ! ApplyCommand(e))
    lastApplied = index
  }

  /**
   * Check if a server can vote another server.
   *
   * @param lastLogTerm  the last log term
   * @param lastLogIndex the last log index
   * @return true if can vote, false if can't vote
   */
  protected def checkElectionRestriction(lastLogTerm: Int, lastLogIndex: Int): Boolean = {
    lastLogTerm >= serverLog.lastTerm && lastLogIndex >= serverLog.lastIndex
  }

  //REQUEST VOTES FROM CANDIDATES
  /**
   * Handles RequestVote message.
   * <p>
   * When a request vote arrives to leader then it has to deny the request or in some special case accept convert and vote.
   *
   * @param requestVote the request vote
   */
  protected def handleRequestVote(requestVote: RequestVote): Unit = {
    requestVote match {
      case RequestVote(candidateTerm, _, _, _) if candidateTerm <= currentTerm => sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case RequestVote(candidateTerm, _, lastLogTerm, lastLogIndex) if checkElectionRestriction(lastLogTerm, lastLogIndex) =>
        voteForApplicantCandidate(candidateTerm)
      case RequestVote(candidateTerm, _, _, _) => becomingFollower(candidateTerm)
        sender() ! RequestVoteResult(voteGranted = false, currentTerm)
      case _ =>
    }
  }

  /**
   * Vote for candidate.
   *
   * @param term the candidate term
   */
  protected def voteForApplicantCandidate(term: Int) {
    becomingFollower(term)
    votedFor = Some(sender().path.name)
    sender() ! RequestVoteResult(voteGranted = true, currentTerm)
  }

  /**
   * Become follower.
   *
   * @param term the candidate term
   */
  protected def becomingFollower(term: Int) {
    currentTerm = term
    context.become(followerBehaviour)
    startTimeoutTimer()
    currentRole = ServerRole.FOLLOWER
  }


  /**
   * Log message with the server role.
   *
   * @param msg the message
   */
  protected def logWithRole(msg: String): Unit = log debug s"${self.path.name}:$currentRole -> $msg"
}

object ServerActor {
  //MESSAGES TO SERVER
  case class IdentifyServer(senderRole: NodeRole) extends ControlMessage
  case class ServerIdentity(name: String) extends ControlMessage
  case class ClientIdentity(name: String) extends ControlMessage

  //FROM STATE MACHINE
  sealed trait InternalMessage
  case class StateMachineResult(indexAndResult: (Int, BankTransactionResult)) extends InternalMessage
  //FROM SELF
  case object SchedulerTick extends InternalMessage
  private case object SchedulerTickKey

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
