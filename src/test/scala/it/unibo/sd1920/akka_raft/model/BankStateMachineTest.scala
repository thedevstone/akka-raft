package it.unibo.sd1920.akka_raft.model

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import it.unibo.sd1920.akka_raft.model.Bank.BankTransactionResult
import it.unibo.sd1920.akka_raft.server.ServerActor.StateMachineResult
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class BankStateMachineTest()
  extends TestKit(ActorSystem("BankStateMachineTest"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  var parent: TestProbe = _
  var stateMachineActor: ActorRef = _
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  override def beforeAll(): Unit = {
    parent = TestProbe()
    stateMachineActor = parent childActorOf(BankStateMachine.props(500.milliseconds), "ActorStateMachine")
  }

  "A State Machine actor with Deposit('A', 100) command" must {
    "send back messages executed to parent" in {
      parent.send(stateMachineActor, BankStateMachine.ApplyCommand(Entry(BankStateMachine.Deposit("A", 100), 0, 0, 256)))
      parent.expectMsg(StateMachineResult(0, BankTransactionResult(Some(100), isSucceeded = true)))
    }
    "send back messages executed to parent after withdrawing" in {
      parent.send(stateMachineActor, BankStateMachine.ApplyCommand(Entry(BankStateMachine.Withdraw("A", 75), 1, 2, 234)))
      parent.expectMsg(StateMachineResult(2, BankTransactionResult(Some(25), isSucceeded = true)))
    }
    "send empty back messages executed to parent after getBalance" in {
      parent.send(stateMachineActor, BankStateMachine.ApplyCommand(Entry(BankStateMachine.GetBalance("B"), 1, 5, 123)))
      parent.expectMsg(StateMachineResult(5, BankTransactionResult(None, isSucceeded = false)))
    }
    "send empty back messages executed to parent after withdraw B" in {
      parent.send(stateMachineActor, BankStateMachine.ApplyCommand(Entry(BankStateMachine.Withdraw("B", 10), 1, 7, 305)))
      parent.expectMsg(StateMachineResult(7, BankTransactionResult(None, isSucceeded = false)))
    }
    "send same response with duplicated request" in {
      parent.send(stateMachineActor, BankStateMachine.ApplyCommand(Entry(BankStateMachine.Withdraw("B", 10), 1, 7, 305)))
      parent.expectMsg(StateMachineResult(7, BankTransactionResult(None, isSucceeded = false)))
    }
  }
}