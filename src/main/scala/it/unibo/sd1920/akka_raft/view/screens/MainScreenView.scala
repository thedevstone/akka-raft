package it.unibo.sd1920.akka_raft.view.screens

import akka.actor.ActorRef
import com.sun.javafx.application.PlatformImpl
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.model.{BankStateMachine, CommandLog}
import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
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
  def updateLogs(serverID: String, commandLog: CommandLog[BankCommand])
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

  // ##################### TO CLIENT ACTOR
  override def log(message: String): Unit = clientActorRef ! ClientActor.Log(message)

  // ##################### FROM CLIENT ACTOR
  override def updateLogs(serverID: String, commandLog: CommandLog[BankStateMachine.BankCommand]): Unit = {
    Platform.runLater(() => {}) //TODO
  }
  override def setViewActorRef(actorRef: ActorRef): Unit = this.clientActorRef = actorRef

  override def addServer(serverID: String): Unit = Platform.runLater(() => {
    addServersToMap(serverID)
    addServerToCombos(serverID)
  })

  override def removeServer(serverID: String): Unit = {} //TODO
}

object MainScreenView {
  def apply(): MainScreenView = {
    PlatformImpl.startup(() => {})
    new MainScreenView()
  }
}