package it.unibo.sd1920.akka_raft.protocol

import akka.dispatch.ControlMessage
import it.unibo.sd1920.akka_raft.model.ServerVolatileState
import it.unibo.sd1920.akka_raft.utils.CommandType.CommandType

object GuiControlMessage {
  sealed trait GuiControlMessage
  //FROM VIEW
  case class GuiStopServer(serverID: String) extends GuiControlMessage with ControlMessage
  case class GuiTimeoutServer(serverID: String) extends GuiControlMessage with ControlMessage
  case class GuiMsgLossServer(serverID: String, value: Double) extends GuiControlMessage with ControlMessage
  case class GuiSendMessage(serverID: String, commandType: CommandType, iban: String, amount: String) extends GuiControlMessage with ControlMessage
  case class Log(message: String) extends GuiControlMessage with ControlMessage
  case class UpdateGui() extends GuiControlMessage with ControlMessage
  //TO VIEW
  case class GuiServerState(serverState: ServerVolatileState) extends GuiControlMessage with ControlMessage
  case class ResultUpdate(id: Int, result: Option[Int]) extends GuiControlMessage with ControlMessage
}