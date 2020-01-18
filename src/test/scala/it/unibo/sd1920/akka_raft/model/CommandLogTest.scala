package it.unibo.sd1920.akka_raft.model

import it.unibo.sd1920.akka_raft.model.BankStateMachine.{BankCommand, Deposit}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec

class CommandLogTest extends AnyFunSpec with BeforeAndAfterEach {
  var commandLog: CommandLog[BankCommand] = _

  override def beforeEach() {
    commandLog = CommandLog.emptyLog()
  }

  override def afterEach() {

  }

  def initWith3Entry(): Unit = {
    val initLog = List[Entry[BankCommand]](new Entry[BankCommand](Deposit("A", 50), 0, 0, 100),
      new Entry[BankCommand](Deposit("A", 50), 1, 1, 101),
      new Entry[BankCommand](Deposit("A", 100), 1, 2, 102))
    commandLog = CommandLog.populatedLog(initLog)
  }

  describe("An empty log") {
    it("should have size == 0") {
      assert(commandLog.size == 0)
    }
    it("should have previous index == -1") {
      assert(commandLog.previousIndex == None)
    }

  }
  describe("A log with 3 entries (testing previous)") {
    it("should have previous index == 2") {
      initWith3Entry()
      assert(commandLog.previousIndex.get == 1)
    }
    it("should have previous of previous term == 0") {
      initWith3Entry()
      assert(commandLog.getEntryAtIndex(commandLog.previousIndex.get - 1).get.term == 0)
    }
  }

  describe("A log with 3 entries (testing last and next)") {
    it("should have lastIndex == 2") {
      initWith3Entry()
      assert(commandLog.lastIndex == 2)
    }
    it("should have lastTerm == 1") {
      initWith3Entry()
      assert(commandLog.lastTerm == 1)
    }
    it("should have nextIndex == 3") {
      initWith3Entry()
      assert(commandLog.nextIndex == 3)
    }
  }

  describe("A log with 3 entries (testing entry at index)") {
    it("should have entry at index 1 == ") {
      initWith3Entry()
      assert(commandLog.getEntryAtIndex(1).get == Entry(Deposit("A", 50), 1, 1, 101))
    }
  }

  describe("A log with 3 entries (committing)") {
    it("should have commit index == -1 ") {
      initWith3Entry()
      assert(commandLog.getCommitIndex == -1)
    }
    it("committing 2 entries should have commit index == 2 ") {
      initWith3Entry()
      commandLog.commit(2)
      assert(commandLog.getCommitIndex == 2)
    }
    it("committing 2 entries should return 2 entries ") {
      initWith3Entry()
      commandLog.commit(1)
      assert(commandLog.committedEntries == List(Entry(Deposit("A", 50), 0, 0, 100), Entry(Deposit("A", 50), 1, 1, 101)))
    }
  }
  describe("A log with 3 entries (deleting)") {
    it("after deleting 2 item should have size == 1 ") {
      initWith3Entry()
      commandLog.remove(1)
      assert(commandLog.size == 1)
    }
    it("after deleting 2 item last index should be == 0 ") {
      initWith3Entry()
      commandLog.remove(1)
      assert(commandLog.lastIndex == 0)
    }
  }
}
