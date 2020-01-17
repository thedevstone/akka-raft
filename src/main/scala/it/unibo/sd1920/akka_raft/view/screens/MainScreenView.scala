package it.unibo.sd1920.akka_raft.view.screens

import akka.actor.ActorRef
import com.sun.javafx.application.PlatformImpl
import it.unibo.sd1920.akka_raft.client.ResultState
import it.unibo.sd1920.akka_raft.model.ServerVolatileState
import it.unibo.sd1920.akka_raft.protocol.GuiControlMessage
import it.unibo.sd1920.akka_raft.utils.CommandType.CommandType
import it.unibo.sd1920.akka_raft.view.utilities.ViewUtilities
import it.unibo.sd1920.akka_raft.view.FXMLScreens
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.layout.BorderPane
import javafx.scene.Scene
import javafx.stage.Stage

trait ClientObserver {
  def setViewActorRef(actorRef: ActorRef): Unit
  def addServer(serverID: String)
  def removeServer(serverID: String)
  def updateServerState(serverID: String, serverVolatileState: ServerVolatileState)
  def updateResultState(requestHistory: Map[Int, ResultState])
}

class MainScreenView extends AbstractMainScreenView() with ClientObserver {
  private var clientActorRef: ActorRef = _
  Platform.runLater(() => this.mainBorder = ViewUtilities.loadFxml(this, FXMLScreens.HOME).asInstanceOf[BorderPane])

  @FXML override def initialize(): Unit = {
    super.initialize()
    val stage = new Stage()
    val scene = new Scene(this.mainBorder)
    stage.setScene(scene)
    ViewUtilities.chargeSceneSheets(scene)
    stage.setOnCloseRequest(_ => System.exit(0))
    stage.show()
  }

  // ##################### FROM CLIENT ACTOR
  override def setViewActorRef(actorRef: ActorRef): Unit = this.clientActorRef = actorRef

  override def addServer(serverID: String): Unit = Platform.runLater(() => {
    addServersToMap(serverID)
    addServerToCombos(serverID)
  })

  override def removeServer(serverID: String): Unit = {} //TODO

  override def updateServerState(serverID: String, serverVolatileState: ServerVolatileState): Unit = {
    Platform.runLater(() => {
      manageServerState(serverID, serverVolatileState)
    })
  }

  override def updateResultState(requestHistory: Map[Int, ResultState]): Unit = {
    Platform.runLater(() => {
      updateResultList(requestHistory: Map[Int, ResultState])
    })
  }

  // ##################### TO CLIENT ACTOR
  override def log(message: String): Unit = clientActorRef ! GuiControlMessage.Log(message)
  override def retryRequest(index: Int, serverID: String): Unit = clientActorRef ! GuiControlMessage.RetryMessage(index, serverID)
  override def requestUpdate(): Unit = clientActorRef ! GuiControlMessage.UpdateGui()
  override def stopServer(serverID: String): Unit = clientActorRef ! GuiControlMessage.GuiStopServer(serverID)
  override def resumeServer(serverID: String): Unit = clientActorRef ! GuiControlMessage.GuiResumeServer(serverID)
  override def timeoutServer(serverID: String): Unit = clientActorRef ! GuiControlMessage.GuiTimeoutServer(serverID)
  override def sendMessage(serverID: String, commandType: CommandType, iban: String, amount: String): Unit =
    clientActorRef ! GuiControlMessage.GuiSendMessage(serverID, commandType, iban, amount)
  override def messageLoss(serverID: String, value: Double): Unit = clientActorRef ! GuiControlMessage.GuiMsgLossServer(serverID, value)
}

object MainScreenView {
  def apply(): MainScreenView = {
    PlatformImpl.startup(() => {})
    new MainScreenView()
  }
}
