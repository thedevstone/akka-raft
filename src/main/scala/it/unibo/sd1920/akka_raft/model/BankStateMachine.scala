package it.unibo.sd1920.akka_raft.model

import akka.actor.{Actor, ActorLogging, Props, Timers}
import it.unibo.sd1920.akka_raft.model.Bank.BankTransactionResult
import it.unibo.sd1920.akka_raft.model.BankStateMachine._
import it.unibo.sd1920.akka_raft.server.ServerActor.StateMachineResult

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
  case object SchedulerTick extends StateMachineMsg

  private sealed trait TimerKey
  private case object SchedulerTickKey extends TimerKey

  def props(schedulerTickPeriod: FiniteDuration): Props = Props(new BankStateMachine(schedulerTickPeriod))
}

class BankStateMachine(schedulerTickPeriod: FiniteDuration) extends Actor with ActorLogging with Timers {
  private val bank: Bank = Bank()
  private var commandQueue: Queue[Entry[BankCommand]] = Queue()
  private var transactionsHistory: Map[TransactionID, BankTransactionResult] = Map()

  override def preStart(): Unit = {
    super.preStart()
    timers startTimerWithFixedDelay(SchedulerTickKey, SchedulerTick, schedulerTickPeriod)
    log info "StateMachine Started"
  }

  override def receive(): Receive = onMessage

  private def onMessage: Receive = {
    case ApplyCommand(entry: Entry[BankCommand]) => commandQueue = commandQueue enqueue entry
    case SchedulerTick => if (commandQueue.nonEmpty) context.parent ! StateMachineResult(applyNextEntry())
  }

  private def applyNextEntry(): (Int, BankTransactionResult) = {
    val dequeued = commandQueue.dequeue
    commandQueue = dequeued._2
    val entry = dequeued._1

    val executionResult: BankTransactionResult = transactionsHistory.get(entry.index) match {
      case None => addTransaction(entry, execute(entry.command))
      case Some(res) => res
    }
    (entry.index, executionResult)
  }

  private def execute(command: BankCommand): BankTransactionResult = command match {
    case BankStateMachine.Withdraw(iban, amount) => bank.withdraw(iban, amount)
    case BankStateMachine.Deposit(iban, amount) => bank.deposit(iban, amount)
    case BankStateMachine.GetBalance(iban) => bank.getBalance(iban)
  }

  def addTransaction(entry: Entry[BankCommand], result: BankTransactionResult): BankTransactionResult = {
    transactionsHistory = transactionsHistory + (entry.index -> result)
    result
  }
}

import it.unibo.sd1920.akka_raft.model.BankStateMachine.{Balance, Iban}

class Bank {
  private var bankAccounts: Map[Iban, Balance] = Map()

  def withdraw(iban: Iban, amount: Int): BankTransactionResult = bankAccounts get iban match {
    case None => getBalance(iban)
    case Some(balance) if balance >= amount => bankAccounts = bankAccounts + (iban -> (balance - amount))
      getBalance(iban)
    case Some(balance) => BankTransactionResult(Some(balance), isSucceeded = false)
  }
  def deposit(iban: Iban, amount: Int): BankTransactionResult = bankAccounts get iban match {
    case None => bankAccounts = bankAccounts + (iban -> amount)
      getBalance(iban)
    case Some(balance) => bankAccounts = bankAccounts + (iban -> (balance + amount))
      getBalance(iban)
  }
  def getBalance(iban: Iban): BankTransactionResult = bankAccounts get iban match {
    case None => BankTransactionResult(None, isSucceeded = false)
    case balance => BankTransactionResult(balance, isSucceeded = true)
  }
}
object Bank {
  case class BankTransactionResult(balance: Option[Balance], isSucceeded: Boolean)
  def apply(): Bank = new Bank()
}



