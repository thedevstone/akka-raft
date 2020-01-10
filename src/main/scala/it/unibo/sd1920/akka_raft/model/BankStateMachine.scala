package it.unibo.sd1920.akka_raft.model

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster

object BankStateMachine {
  type Iban = String
  type Balance = Int

  sealed trait BankCommand
  case class Withdraw(iban: Iban, amount: Int) extends BankCommand
  case class Deposit(iban: Iban, amount: Int) extends BankCommand
  case class GetBalance() extends BankCommand

  sealed trait StateMachineMsg
  case class ExecuteCommand(entry :Entry[BankCommand]) extends StateMachineMsg
  case class CommandResult(result:(Boolean,Int)) extends StateMachineMsg
  case class GetLastCommandExecuted(lastIndex:Int) extends StateMachineMsg

  def props: Props = Props(new BankStateMachine())
}

private class BankStateMachine extends Actor  with ActorLogging {
  protected[this] val cluster: Cluster = Cluster(context.system)
  private final val MIN_WORK_DELAY = 500
  private final val MAX_WORK_DELAY = 2000

  override def receive: Receive = ???

  private def execute(index: Int, command: (String, Int)): (Boolean, Int) = ???
}

import it.unibo.sd1920.akka_raft.model.BankStateMachine.{Balance, Iban}
private class Bank {
  private var bankAccounts: Map[Iban, Balance] = Map()

  def withdraw(iban: Iban, amount: Int): Option[Balance] = bankAccounts get iban match {
    case None => None
    case Some(balance) => bankAccounts = bankAccounts + (iban -> (balance - amount))
      Some(balance - amount)
  }
  def deposit(iban: Iban, amount: Int): Balance = bankAccounts get iban match {
    case None => bankAccounts = bankAccounts + (iban -> amount)
      amount
    case Some(balance) => bankAccounts = bankAccounts + (iban -> (balance + amount)) //si può andare in rosso
      balance + amount
  }
  def getBalance(iban: Iban): Option[Balance] = bankAccounts get iban
}

