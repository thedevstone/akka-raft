package it.unibo.sd1920.akka_raft.client

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class ClientActorTest
  extends TestKit(ActorSystem("ClientGuiActorTest"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  var clientGuiActor: ActorRef = _

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  override def beforeAll(): Unit = {
    clientGuiActor = system actorOf(ClientActor.props, "ClientGuiActor")
  }

  "A Client Gui Actor that send message to gui " must {
    "send messages " in {
      
    }
  }
}
