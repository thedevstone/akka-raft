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

/**
 * Bank State Machine Actor that emulate a state machine behaviour.
 */
class BankStateMachine(schedulerTickPeriod: FiniteDuration) extends Actor with ActorLogging with Timers {
  private val bank: Bank = Bank()
  private var commandQueue: Queue[Entry[BankCommand]] = Queue()
  private var transactionsHistory: Map[TransactionID, BankTransactionResult] = Map()

  /**
   * Init the scheduler that elaborate request on every schedule tick.
   */
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

  /**
   * Apply the next command present in the queue.
   *
   * - The state machine get the next message from the queue.
   * - Then it execute the command and put the result in a transaction cache.
   * - If a request is executed then the result is immediately returned.
   *
   * @return
   */
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

  /**
   * Execute the specific command on the bank instance.
   *
   * @param command the command
   * @return the transaction result from the bank
   */
  private def execute(command: BankCommand): BankTransactionResult = command match {
    case BankStateMachine.Withdraw(iban, amount) => bank.withdraw(iban, amount)
    case BankStateMachine.Deposit(iban, amount) => bank.deposit(iban, amount)
    case BankStateMachine.GetBalance(iban) => bank.getBalance(iban)
  }

  /**
   * Add the transaction to the transaction cache.
   *
   * @param entry  the entry from the log
   * @param result the result form the bank
   * @return the transaction result
   */
  def addTransaction(entry: Entry[BankCommand], result: BankTransactionResult): BankTransactionResult = {
    transactionsHistory = transactionsHistory + (entry.index -> result)
    result
  }
}

import it.unibo.sd1920.akka_raft.model.BankStateMachine.{Balance, Iban}

/**
 * A simple bank implementation.
 */
class Bank {
  private var bankAccounts: Map[Iban, Balance] = Map()
  /**
   * Withdraw an amount of money from a specific account, given the account iban.
   *
   * @param iban   the transaction iban
   * @param amount the transaction amount
   * @return the transaction result
   */
  def withdraw(iban: Iban, amount: Int): BankTransactionResult = bankAccounts get iban match {
    case None => getBalance(iban)
    case Some(balance) if balance >= amount => bankAccounts = bankAccounts + (iban -> (balance - amount))
      getBalance(iban)
    case Some(balance) => BankTransactionResult(Some(balance), isSucceeded = false)
  }
  /**
   * Deposit an amount of money from a specific account, given the account iban.
   *
   * @param iban   the transaction iban
   * @param amount the transaction amount
   * @return the transaction result
   */
  def deposit(iban: Iban, amount: Int): BankTransactionResult = bankAccounts get iban match {
    case None => bankAccounts = bankAccounts + (iban -> amount)
      getBalance(iban)
    case Some(balance) => bankAccounts = bankAccounts + (iban -> (balance + amount))
      getBalance(iban)
  }
  /**
   * Get balance of a specific account, given the account iban.
   *
   * @param iban the transaction iban
   * @return the transaction result
   */
  def getBalance(iban: Iban): BankTransactionResult = bankAccounts get iban match {
    case None => BankTransactionResult(None, isSucceeded = false)
    case balance => BankTransactionResult(balance, isSucceeded = true)
  }
}
/**
 * Factory for bank.
 */
object Bank {
  case class BankTransactionResult(balance: Option[Balance], isSucceeded: Boolean)
  def apply(): Bank = new Bank()
}



