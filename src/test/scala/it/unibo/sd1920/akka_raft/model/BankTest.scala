package it.unibo.sd1920.akka_raft.model

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.BeforeAndAfterEach

class BankTest extends AnyFunSpec with BeforeAndAfterEach {
  var bank: Bank = _

  override def beforeEach(): Unit = {
    bank = Bank()
  }

  override def afterEach(): Unit = {

  }

  describe("Depositing 5 to a bank of iban A") {
    it("should return balance == 5") {
      assert(bank.deposit("A", 5).balance.get == 5)
    }
  }

  describe("Getting balance from a null account") {
    it("should return balance == None") {
      val result = bank.getBalance("B")
      assert(result.balance.isEmpty && !result.isSucceeded)
    }
  }

  describe("Withdrawing 5 to a bank iban A") {
    it("should return balance == None") {
      val result = bank.withdraw("A", 5)
      assert(result.balance.isEmpty && !result.isSucceeded)
    }
    it("should return 0 after depositing 5 to A") {
      bank.deposit("A", 5)
      val result = bank.withdraw("A", 5)
      assert(result.balance.contains(0) && result.isSucceeded)
    }
    it("should return -5 after withdrawing 5 to A") {
      bank.deposit("A", 5)
      bank.withdraw("A", 5)
      val result = bank.withdraw("A", 5)
      assert(result.balance.contains(0) && !result.isSucceeded)
    }
  }
}
