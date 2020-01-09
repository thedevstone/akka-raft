package it.unibo.sd1920.akka_raft.model

import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec

import scala.collection.mutable.ArrayBuffer

class CommandLogTest extends AnyFunSpec with BeforeAndAfterEach {
  var commandLog: CommandLog[String] = _

  override def beforeEach() {
    commandLog = CommandLog.emptyLog()
  }

  override def afterEach() {

  }

  def initWith3Entry(): Unit = {
    var initLog = ArrayBuffer[Entry[String]](new Entry[String]("push", 0, 1, 100),
      new Entry[String]("push", 0, 2, 101),
      new Entry[String]("pop", 1, 3, 102))
    commandLog = CommandLog.populatedLog(initLog)
  }

  describe("A empty log") {
    it("should have size == 0") {
      assert(commandLog.length == 0)
    }
    it("should have new size == 1") {
      commandLog.append(new Entry[String]("push", 0, 1, 101))
      commandLog.append(new Entry[String]("push", 0, 2, 102))
      assert(commandLog.length == 2)
    }
  }

}
