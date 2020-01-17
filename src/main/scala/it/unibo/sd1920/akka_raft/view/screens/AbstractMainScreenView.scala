package it.unibo.sd1920.akka_raft.view.screens

import com.jfoenix.controls._
import eu.hansolo.enzo.led.Led
import it.unibo.sd1920.akka_raft.client.ResultState
import it.unibo.sd1920.akka_raft.model.{BankStateMachine, Entry, ServerVolatileState}
import it.unibo.sd1920.akka_raft.model.BankStateMachine.{BankCommand, Withdraw}
import it.unibo.sd1920.akka_raft.utils.{CommandType, ServerRole}
import it.unibo.sd1920.akka_raft.utils.CommandType.CommandType
import it.unibo.sd1920.akka_raft.view.utilities.{JavafxEnums, ViewUtilities}
import javafx.fxml.FXML
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.Label
import javafx.scene.layout.{BorderPane, HBox, VBox}
import javafx.scene.text.Font
import org.kordamp.ikonli.ionicons.Ionicons
import org.kordamp.ikonli.material.Material

trait View {
  def log(message: String): Unit
  def retryRequest(index: Int, serverID: String): Unit
  def requestUpdate(): Unit
  def stopServer(serverID: String): Unit
  def timeoutServer(serverID: String): Unit
  def sendMessage(serverID: String, commandType: CommandType, iban: String, amount: String)
  def messageLoss(serverID: String, value: Double): Unit
}

abstract class AbstractMainScreenView extends View {
  @FXML protected var mainBorder: BorderPane = _
  @FXML protected var vBoxServerNames: VBox = _
  @FXML protected var vBoxServerLogs: VBox = _
  @FXML protected var serverIDCombo: JFXComboBox[String] = _
  //STATE LABELS
  @FXML protected var stateLabelRole: Label = _
  @FXML protected var stateLabelLastApplied: Label = _
  @FXML protected var stateLabelVotedFor: Label = _
  @FXML protected var stateLabelCurrentTerm: Label = _
  @FXML protected var stateLabelNextIndex: Label = _
  @FXML protected var stateLabelLastMatched: Label = _
  @FXML protected var stateLabelLastCommitted: Label = _
  //SETTINGS
  @FXML protected var sliderMsgLoss: JFXSlider = _
  @FXML protected var buttonStop: JFXButton = _
  @FXML protected var buttonTimeout: JFXButton = _
  //REQUEST
  @FXML protected var comboCommand: JFXComboBox[CommandType] = _
  @FXML protected var textFieldIban: JFXTextField = _
  @FXML protected var textFieldAmount: JFXTextField = _
  @FXML protected var buttonSend: JFXButton = _
  //RESULTS
  @FXML protected var listViewResult: JFXListView[String] = _
  @FXML protected var radioButtonExecuted: JFXRadioButton = _

  type HBoxServerID = HBox
  type HBoxServerLog = HBox
  protected var serverToHBox: Map[String, (HBoxServerID, HBoxServerLog)] = Map()
  protected var serverToState: Map[String, ServerVolatileState] = Map()

  @FXML def initialize(): Unit = {
    this.initButtons()
    this.initCombos()
    this.initSlider()
    this.initListView()
    this.assertNodeInjected()
  }

  private def assertNodeInjected(): Unit = {
    assert(mainBorder != null, "fx:id=\"mainBorder\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(vBoxServerNames != null, "fx:id=\"vBoxServerNames\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(vBoxServerLogs != null, "fx:id=\"vBoxServerLogs\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(serverIDCombo != null, "fx:id=\"serverStateCombo\" was not injected: check your FXML file 'MainScreen.fxml'.")
  }

  protected def showPopupInfo(): Unit = {
    ViewUtilities.showNotificationPopup("Help", "Click '^' and create a configuration \nRight Click on screen hides toolbar",
      JavafxEnums.LONG_DURATION, JavafxEnums.INFO_NOTIFICATION, null)
  }

  private def initButtons(): Unit = {
    this.buttonSend.setGraphic(ViewUtilities.iconSetter(Material.SEND, JavafxEnums.MEDIUM_ICON))
    this.buttonSend.setOnAction(_ => sendMessage(getSelectedServer, comboCommand.getSelectionModel.getSelectedItem, textFieldIban.getText, textFieldAmount.getText))
    this.buttonTimeout.setOnAction(_ => timeoutServer(getSelectedServer))
    this.buttonStop.setOnAction(_ => {
      stopServer(getSelectedServer)
      val state: ServerVolatileState = ServerVolatileState(ServerRole.CANDIDATE, 1, 1, Some("S1"), 2, 3, 3,
        List(Entry[BankCommand](Withdraw("ciao", 2), 2, 3, 123),
          Entry[BankCommand](BankStateMachine.Deposit("ciao", 34), 5, 2, 133),
          Entry[BankCommand](BankStateMachine.Deposit("ciao", 34), 6, 2, 134)))
      this.manageServerState("S1", state)
    })
    this.radioButtonExecuted.setText("Done")
    this.radioButtonExecuted.setOnAction(_ => {
      requestUpdate()
      if (radioButtonExecuted.isSelected) {
        this.radioButtonExecuted.setText("Done")
      } else {
        this.radioButtonExecuted.setText("Running")
      }
    })
  }

  private def initCombos(): Unit = {
    this.serverIDCombo.getSelectionModel.selectedItemProperty()
      .addListener((_, _, newState) => {
        serverToState.get(newState) match {
          case None => ViewUtilities.showNotificationPopup("Server Errorr", "No server update present", JavafxEnums.MEDIUM_DURATION, JavafxEnums.ERROR_NOTIFICATION, null)
          case Some(state) => updateServerState(state)
        }
      })
    CommandType.values.foreach(this.comboCommand.getItems.add(_))
    this.comboCommand.getSelectionModel.select(0)
  }

  private def initSlider(): Unit = {
    this.sliderMsgLoss.setOnMouseReleased(_ => messageLoss(getSelectedServer, this.sliderMsgLoss.getValue / 100))
  }

  private def initListView(): Unit = {
    this.listViewResult.getSelectionModel.selectedIndexProperty().addListener((_, _, newValue) => {
      if (newValue.intValue() != -1 && this.radioButtonExecuted.isSelected) {
        retryRequest(newValue.intValue(), getSelectedServer)
      }
    })
  }

  private def getSelectedServer: String = serverIDCombo.getSelectionModel.getSelectedItem

  def addServersToMap(serverID: String): Unit = {
    //ID NODE
    val serverIDNode = new HBox()
    val labelIDNode = new Label(serverID)
    labelIDNode.setGraphic(ViewUtilities.iconSetter(Ionicons.ION_CUBE, JavafxEnums.BIGGER_ICON))
    serverIDNode.setMinHeight(JavafxEnums.BIGGER_ICON.dim)
    serverIDNode.setMaxHeight(JavafxEnums.BIGGER_ICON.dim)
    serverIDNode.getChildren.add(labelIDNode)
    //LOG NODE
    val serverLogNode = new HBox()
    serverLogNode.setMinHeight(JavafxEnums.BIGGER_ICON.dim)
    serverLogNode.setMaxHeight(JavafxEnums.BIGGER_ICON.dim)
    serverLogNode.setSpacing(10)
    serverLogNode.setPadding(new Insets(5, 5, 5, 5))
    //ADDING
    this.vBoxServerNames.getChildren.add(serverIDNode)
    this.vBoxServerLogs.getChildren.add(serverLogNode)
    this.serverToHBox = this.serverToHBox + (serverID -> (serverIDNode, serverLogNode))
  }

  protected def addServerToCombos(serverID: String): Unit = {
    this.serverIDCombo.getItems.add(serverID)
    this.serverIDCombo.getSelectionModel.select(0)
  }

  protected def manageServerState(serverID: String, serverVolatileState: ServerVolatileState): Unit = {
    serverToHBox.get(serverID) match {
      case None => new IllegalStateException("You sent a serverID that does not exist in map")
      case Some(entry) =>
        //UPDATING INFO
        this.serverToState = this.serverToState + (serverID -> serverVolatileState)
        //UPDATING LOG
        entry._2.getChildren.clear()
        serverVolatileState.commandLog.foreach(c => {
          val ledOn = entry._2.getChildren.size() <= serverVolatileState.lastCommitted
          val entryToAdd = new EntryBox(s"${serverVolatileState.currentTerm} : ${c.command.toString.substring(0, 1)}", ledOn)
          entry._2.getChildren.add(entryToAdd)
        })
        if (this.serverIDCombo.getSelectionModel.getSelectedItem == serverID) {
          updateServerState(serverVolatileState)
        }
    }
  }

  private def updateServerState(serverVolatileState: ServerVolatileState): Unit = {
    this.stateLabelCurrentTerm.setText(serverVolatileState.currentTerm.toString)
    this.stateLabelLastApplied.setText(serverVolatileState.lastApplied.toString)
    this.stateLabelVotedFor.setText(serverVolatileState.votedFor.getOrElse(""))
    this.stateLabelNextIndex.setText(serverVolatileState.nextIndexToSend.toString)
    this.stateLabelRole.setText(serverVolatileState.role.toString)
    this.stateLabelLastCommitted.setText(serverVolatileState.lastCommitted.toString)
    this.stateLabelLastMatched.setText(serverVolatileState.lastMatchedEntry.toString)
  }

  def updateResultList(requestHistory: Map[Int, ResultState]): Unit = {
    this.listViewResult.getItems.clear()
    requestHistory.toList.filter(t => {
      if (!this.radioButtonExecuted.isSelected) {
        !t._2.executed
      } else {
        true
      }
    }).foreach(e => this.listViewResult.getItems
      .add(s"ID: ${e._1} -> [CMD: ${e._2.command}] [Ex: ${e._2.executed}] [Res: ${e._2.result.getOrElse("Not Executed")}]"))
  }
}

class EntryBox(info: String, on: Boolean) extends VBox {
  this.setAlignment(Pos.CENTER)
  val entryLed = new Led()
  entryLed.setOn(on)
  entryLed.setMinSize(40, 40)
  val entryInfo = new Label(info)
  entryInfo.setFont(new Font(12))
  this.getChildren.addAll(entryLed, entryInfo)
}
