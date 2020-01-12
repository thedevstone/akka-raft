package it.unibo.sd1920.akka_raft.model

import akka.actor.{Actor, ActorLogging, Props, Timers}
import it.unibo.sd1920.akka_raft.model.BankStateMachine._

import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration

object BankStateMachine {
  type Iban = String
  type Balance = Int
  type TransactionID = Int

  sealed trait BankCommand
  case class Withdraw(iban: Iban, amount: Int) extends BankCommand
  case class Deposit(iban: Iban, amount: Int) extends BankCommand
  case class GetBalance(iban: Iban) extends BankCommand

  sealed trait StateMachineMsg
  case class ApplyCommand(entry: Entry[BankCommand]) extends StateMachineMsg
  case class CommandResult(result: (Int, Option[Int])) extends StateMachineMsg
  case object SchedulerTick extends StateMachineMsg

  private sealed trait TimerKey
  private case object SchedulerTickKey extends TimerKey

  def props(schedulerTickPeriod: FiniteDuration): Props = Props(new BankStateMachine(schedulerTickPeriod))
}

class BankStateMachine(schedulerTickPeriod: FiniteDuration) extends Actor with ActorLogging with Timers {
  private val bank: Bank = Bank()
  private var commandQueue: Queue[Entry[BankCommand]] = Queue()
  private var transactionsHistory: Map[TransactionID, Option[Int]] = Map()

  override def preStart(): Unit = {
    super.preStart()
    timers startTimerWithFixedDelay(SchedulerTickKey, SchedulerTick, schedulerTickPeriod)
  }

  override def receive(): Receive = onMessage

  private def onMessage: Receive = {
    case ApplyCommand(entry: Entry[BankCommand]) => commandQueue = commandQueue enqueue entry
    case SchedulerTick => if (commandQueue.nonEmpty) context.parent ! CommandResult(applyNextEntry())
  }

  private def applyNextEntry(): (Int, Option[Int]) = {
    val dequeued = commandQueue.dequeue
    commandQueue = dequeued._2
    val entry = dequeued._1

    val executionResult: Option[Int] = transactionsHistory.get(entry.index) match {
      case None => addTransaction(entry, execute(entry.command))
      case Some(res) => res
    }
    (entry.index, executionResult)
  }

  private def execute(command: BankCommand): Option[Int] = command match {
    case BankStateMachine.Withdraw(iban, amount) => bank.withdraw(iban, amount)
    case BankStateMachine.Deposit(iban, amount) => bank.deposit(iban, amount)
    case BankStateMachine.GetBalance(iban) => bank.getBalance(iban)
  }

  def addTransaction(entry: Entry[BankCommand], result: Option[Int]): Option[Int] = {
    transactionsHistory = transactionsHistory + (entry.index -> result)
    result
  }
}

import it.unibo.sd1920.akka_raft.model.BankStateMachine.{Balance, Iban}

class Bank {
  private var bankAccounts: Map[Iban, Balance] = Map()

  def withdraw(iban: Iban, amount: Int): Option[Balance] = bankAccounts get iban match {
    case None => None
    case Some(balance) => bankAccounts = bankAccounts + (iban -> (balance - amount)) //si può andare in rosso
      getBalance(iban)
  }
  def deposit(iban: Iban, amount: Int): Option[Balance] = bankAccounts get iban match {
    case None => bankAccounts = bankAccounts + (iban -> amount)
      getBalance(iban)
    case Some(balance) => bankAccounts = bankAccounts + (iban -> (balance + amount)) //si può andare in rosso
      getBalance(iban)
  }
  def getBalance(iban: Iban): Option[Balance] = bankAccounts get iban
}
object Bank {
  def apply(): Bank = new Bank()
}



