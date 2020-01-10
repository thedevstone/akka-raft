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
      assert(bank.deposit("A", 5).get == 5)
    }
  }

  describe("Getting balance from a null account") {
    it("should return balance == None") {
      var balance = bank.getBalance("B")
      assert(balance == None)
    }
  }

  describe("Withdrawing 5 to a bank iban A") {
    it("should return balance == None") {
      var balance = bank.withdraw("A", 5)
      assert(balance.isEmpty)
    }
    it("should return 0 after depositing 5 to A") {
      bank.deposit("A", 5)
      var balance = bank.withdraw("A", 5)
      assert(balance.contains(0))
    }
    it("should return -5 after withdrawing 5 to A") {
      bank.deposit("A", 5)
      bank.withdraw("A", 5)
      var balance = bank.withdraw("A", 5)
      assert(balance.contains(-5))
    }
  }
}
