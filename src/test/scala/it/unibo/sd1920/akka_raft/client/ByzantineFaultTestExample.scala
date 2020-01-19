package it.unibo.sd1920.akka_raft.client

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import it.unibo.sd1920.akka_raft.model.BankStateMachine.{BankCommand, Deposit, GetBalance, Withdraw}
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.protocol.{AppendEntries, RequestVote}
import it.unibo.sd1920.akka_raft.protocol.GuiControlMessage.{GuiSendMessage, GuiTimeoutServer}
import it.unibo.sd1920.akka_raft.server.ServerActor
import it.unibo.sd1920.akka_raft.utils.{CommandType, NetworkConstants}
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Random
//Questo test visivo molto grezzo, concepito per mettere in luce in maniera evidente la non resistenza di RAFT a bizzantine failure.
//Il test inizializza un cluster di server che riceve messaggi da propagare nel tempo, per ogni messaggio inviato al cluster,
//Un server casuale genera un messaggio random che viene spedito ad un mittente random, questo porta a comportamenti anomali,
//e non contemplati.
class ByzantineFaultTestExample
  extends TestKit(ActorSystem("ClientGuiActorTest"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  var clientGuiActor: ActorRef = _
  var serverActor0: ActorRef = _
  var serverActor1: ActorRef = _
  var serverActor2: ActorRef = _
  var serverActor3: ActorRef = _
  var serverActor4: ActorRef = _
  var serversRef: List[ActorRef] = _
  var serversName: List[String] = _

  //Spengo il sistema
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
    Thread.sleep(50000)
  }

  private def getConfig(config: String): com.typesafe.config.Config = {
    var port = config
    if(port.isEmpty) port = "0"
    ConfigFactory.parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load("server"))
  }

  private def getRandomServerName: String = {
    serversName(Random.nextInt(serversName.size))
  }


  private def getRandomServerRef: ActorRef = {
    serversRef(Random.nextInt(serversRef.size))
  }
  private def getRandomCommand: BankCommand = {

    def value:Int = Random.nextInt(30)
    if(value >= 20) {
      Withdraw(Random.nextInt(1000).toString, Random.nextInt(1000))
    }else if (value >= 10){
      Deposit(Random.nextInt(1000).toString, Random.nextInt(1000))
    }else{
      GetBalance(Random.nextInt(1000).toString)
    }


  }

  private def getRandomEntry: Option[Entry[BankCommand]] = {
    if(Random.nextBoolean()){
      Some(Entry[BankCommand](getRandomCommand, Random.nextInt(1000), Random.nextInt(1000), Random.nextInt(1000)))
    }else{
      None
    }
  }

  private def getRandomMessage(actorRef: ActorRef): Any = {
    if(Random.nextBoolean()){
      RequestVote(Random.nextInt(1000), actorRef, Random.nextInt(1000), Random.nextInt(1000))
    }else{
      AppendEntries(Random.nextInt(1000), getRandomEntry, getRandomEntry ,Random.nextInt(1000))
    }
  }

  private def randomBehavior(actorRef: ActorRef, name: String): Any = {

    Stream.range(0,Random.nextInt(6)).foreach(_ => {
      if(Random.nextBoolean()) clientGuiActor.tell(GuiTimeoutServer(name),clientGuiActor)
      val byzantineTarget: ActorRef = getRandomServerRef
      byzantineTarget.tell(getRandomMessage(actorRef),actorRef)
    })
  }

  //Inizializzo il cluster generando 5 e un solo client
  override def beforeAll(): Unit = {
    serversName = List("S0","S1","S2","S3","S4")
    serversRef = List()

    val clientSystem0 = ActorSystem(NetworkConstants.clusterName, ConfigFactory.parseString("""akka.remote.artery.canonical.port=5000""")
      .withFallback(ConfigFactory.load("client")))
    clientGuiActor = clientSystem0 actorOf(ClientActor.props, "C0")
    val serverSystem0 = ActorSystem(NetworkConstants.clusterName, getConfig(NetworkConstants.secondSeedPort.toString))
    serverActor0 = serverSystem0 actorOf(ServerActor.props, "S0")
    serversRef :::= List(serverActor0)

    val serverSystem1 = ActorSystem(NetworkConstants.clusterName, getConfig(""))
    serverActor1 = serverSystem1 actorOf(ServerActor.props, "S1")
    serversRef :::= List(serverActor1)
    val serverSystem2 = ActorSystem(NetworkConstants.clusterName, getConfig(""))
    serverActor2 = serverSystem2 actorOf(ServerActor.props, "S2")
    serversRef :::= List(serverActor2)
    val serverSystem3 = ActorSystem(NetworkConstants.clusterName, getConfig(""))
    serverActor3 = serverSystem3 actorOf(ServerActor.props, "S3")
    serversRef :::= List(serverActor3)
    val serverSystem4 = ActorSystem(NetworkConstants.clusterName, getConfig(""))
    serverActor4 = serverSystem4 actorOf(ServerActor.props, "S4")
    serversRef :::= List(serverActor4)
  }

  "Testing Cluster long running" must {
    "" in {
      //Attendo che tutto sia online
      Thread.sleep(20000)
      //S0 è considerato il server byzzantino
      //Sottometto al cluster una serie di richieste, seguite da una serie di messaggi "bizzantini", messaggi generati e spediti con
      //L'indirizzo di in un server casuale che portano scompiglio, l'effetto più immediato è l'assenza nel server di un Leader fisso.
      //Possono presentarsi anche comportamenti non conteplati.
      Stream.range(0,40).foreach(_ => {
        clientGuiActor.tell(GuiSendMessage(getRandomServerName , CommandType(Random.nextInt(3)), Random.nextInt(100).toString , Random.nextInt(100).toString), clientGuiActor)
        randomBehavior(serverActor0, serverActor0.path.name)
        Thread.sleep(500)
      })

      Thread.sleep(5000)
    }
  }
}


