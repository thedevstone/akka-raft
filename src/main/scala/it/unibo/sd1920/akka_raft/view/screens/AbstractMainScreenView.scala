package it.unibo.sd1920.akka_raft.view.screens

import com.jfoenix.controls._
import eu.hansolo.enzo.led.Led
import it.unibo.sd1920.akka_raft.client.ResultState
import it.unibo.sd1920.akka_raft.model.ServerVolatileState
import it.unibo.sd1920.akka_raft.utils.CommandType
import it.unibo.sd1920.akka_raft.utils.CommandType.CommandType
import it.unibo.sd1920.akka_raft.view.utilities.{JavafxEnums, ViewUtilities}
import javafx.fxml.FXML
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Label, ScrollPane, Tooltip}
import javafx.scene.layout.{BorderPane, HBox, VBox}
import javafx.scene.text.Font
import org.kordamp.ikonli.ionicons.Ionicons
import org.kordamp.ikonli.material.Material

trait View {
  def log(message: String): Unit
  def retryRequest(index: Int, serverID: String): Unit
  def requestUpdate(): Unit
  def stopServer(serverID: String): Unit
  def resumeServer(serverID: String): Unit
  def timeoutServer(serverID: String): Unit
  def sendMessage(serverID: String, commandType: CommandType, iban: String, amount: String)
  def messageLoss(serverID: String, value: Double): Unit
}

abstract class AbstractMainScreenView extends View {
  @FXML protected var mainBorder: BorderPane = _
  @FXML protected var vBoxServerNames: VBox = _
  @FXML protected var vBoxServerLogs: VBox = _
  @FXML protected var serverIDCombo: JFXComboBox[String] = _
  @FXML protected var borderLog: BorderPane = _
  @FXML protected var scrollLog: ScrollPane = _
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
  protected var serverToSettings: Map[String, ServerSettings] = Map()

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
    assert(serverIDCombo != null, "fx:id=\"serverIDCombo\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(stateLabelRole != null, "fx:id=\"stateLabelRole\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(stateLabelLastApplied != null, "fx:id=\"stateLabelLastApplied\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(stateLabelVotedFor != null, "fx:id=\"stateLabelVotedFor\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(stateLabelCurrentTerm != null, "fx:id=\"stateLabelCurrentTerm\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(stateLabelNextIndex != null, "fx:id=\"stateLabelNextIndex\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(stateLabelLastMatched != null, "fx:id=\"stateLabelLastMatched\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(stateLabelLastCommitted != null, "fx:id=\"stateLabelLastCommitted\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(sliderMsgLoss != null, "fx:id=\"sliderMsgLoss\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(buttonStop != null, "fx:id=\"buttonStop\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(buttonTimeout != null, "fx:id=\"buttonTimeout\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(comboCommand != null, "fx:id=\"comboCommand\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(textFieldIban != null, "fx:id=\"textFieldIban\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(textFieldAmount != null, "fx:id=\"textFieldAmount\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(buttonSend != null, "fx:id=\"buttonSend\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(radioButtonExecuted != null, "fx:id=\"radioButtonExecuted\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(listViewResult != null, "fx:id=\"listViewResult\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(borderLog != null, "fx:id=\"borderLog\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(scrollLog != null, "fx:id=\"scrollLog\" was not injected: check your FXML file 'MainScreen.fxml'.")
  }

  protected def showPopupInfo(): Unit = {
    ViewUtilities.showNotificationPopup("Help", "Click '^' and create a configuration \nRight Click on screen hides toolbar",
      JavafxEnums.LONG_DURATION, JavafxEnums.INFO_NOTIFICATION, null)
  }

  private def initButtons(): Unit = {
    scrollLog.hvalueProperty().bind(borderLog.widthProperty())
    this.buttonSend.setGraphic(ViewUtilities.iconSetter(Material.SEND, JavafxEnums.MEDIUM_ICON))
    this.buttonSend.setOnAction(_ => sendMessage(getSelectedServer, comboCommand.getSelectionModel.getSelectedItem, textFieldIban.getText, textFieldAmount.getText))
    this.buttonTimeout.setTooltip(new Tooltip("Server timeout"))
    this.buttonTimeout.setGraphic(ViewUtilities.iconSetter(Material.TIMER_OFF, JavafxEnums.MEDIUM_ICON))
    this.buttonTimeout.setOnAction(_ => timeoutServer(getSelectedServer))
    this.buttonStop.setTooltip(new Tooltip("Server start stop"))
    this.buttonStop.setOnAction(_ => {
      serverToSettings.get(getSelectedServer) match {
        case None => ViewUtilities.showNotificationPopup("Server Errorr", "Wait servers to join the cluster", JavafxEnums.MEDIUM_DURATION, JavafxEnums.ERROR_NOTIFICATION, null)
        case Some(s) =>
          val selectedServer = getSelectedServer
          if (s.stopped) {
            resumeServer(selectedServer)
          } else {
            stopServer(selectedServer)
          }
          val newSetting = ServerSettings(s.lossValue, !s.stopped)
          serverToSettings = serverToSettings + (selectedServer -> newSetting)
          updateServerSettings(newSetting)
      }
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
    this.serverIDCombo.getSelectionModel.selectedItemProperty().addListener((_, _, newState) => {
      serverToState.get(newState) match {
        case None => ViewUtilities.showNotificationPopup("Server Errorr", "No server update present", JavafxEnums.MEDIUM_DURATION, JavafxEnums.ERROR_NOTIFICATION, null)
        case Some(state) => updateServerState(state)
      }
      serverToSettings.get(newState) match {
        case None => ViewUtilities.showNotificationPopup("Server Errorr", "No server update present", JavafxEnums.MEDIUM_DURATION, JavafxEnums.ERROR_NOTIFICATION, null)
        case Some(settings) => updateServerSettings(settings)
      }
    })
    CommandType.values.foreach(this.comboCommand.getItems.add(_))
    this.comboCommand.getSelectionModel.select(0)
  }

  private def initSlider(): Unit = {
    this.sliderMsgLoss.setOnMouseReleased(_ => {
      val newLoss = this.sliderMsgLoss.getValue
      val oldEntry = serverToSettings(getSelectedServer)
      messageLoss(getSelectedServer, 1 - (newLoss / 100))
      serverToSettings = serverToSettings + (getSelectedServer -> ServerSettings(newLoss, oldEntry.stopped))
    })
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
    //ADDING SERVER TO SETTINGS
    val newSetting = ServerSettings(0, stopped = false)
    this.serverToSettings = this.serverToSettings + (serverID -> newSetting)
    updateServerSettings(newSetting)
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
          val entryToAdd = new EntryBox(s"${c.term} : ${c.command.toString.substring(0, 1)}", ledOn)
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

  private def updateServerSettings(settings: ServerSettings): Unit = {
    this.sliderMsgLoss.setValue(settings.lossValue)
    if (settings.stopped) {
      this.buttonStop.setGraphic(ViewUtilities.iconSetter(Material.PLAY_ARROW, JavafxEnums.MEDIUM_ICON))
    } else {
      this.buttonStop.setGraphic(ViewUtilities.iconSetter(Material.STOP, JavafxEnums.MEDIUM_ICON))
    }
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

case class ServerSettings(lossValue: Double, stopped: Boolean)
