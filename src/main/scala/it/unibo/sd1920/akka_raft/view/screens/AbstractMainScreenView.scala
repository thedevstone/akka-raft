package it.unibo.sd1920.akka_raft.view.screens

import com.jfoenix.controls.JFXComboBox
import it.unibo.sd1920.akka_raft.view.utilities.{JavafxEnums, ViewUtilities}
import javafx.fxml.FXML
import javafx.scene.control.{Label, ScrollPane}
import javafx.scene.control.ScrollPane.ScrollBarPolicy
import javafx.scene.layout.{BorderPane, HBox, VBox}
import org.kordamp.ikonli.ionicons.Ionicons

trait View {
  def log(message: String): Unit
}

abstract class AbstractMainScreenView extends View {
  @FXML protected var mainBorder: BorderPane = _
  @FXML protected var vBoxServerNames: VBox = _
  @FXML protected var vBoxServerLogs: VBox = _
  @FXML protected var serverStateCombo: JFXComboBox[String] = _
  @FXML protected var serverCommandCombo: JFXComboBox[String] = _

  type HBoxServerID = HBox
  type HBoxServerLog = HBox
  protected var serverToHBox: Map[String, (HBoxServerID, HBoxServerLog)] = Map()

  @FXML def initialize(): Unit = {
    this.assertNodeInjected()
    this.initCombos()
  }

  private def assertNodeInjected(): Unit = {
    assert(mainBorder != null, "fx:id=\"mainBorder\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(vBoxServerNames != null, "fx:id=\"vBoxServerNames\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(vBoxServerLogs != null, "fx:id=\"vBoxServerLogs\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(serverStateCombo != null, "fx:id=\"serverStateCombo\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(serverCommandCombo != null, "fx:id=\"serverCommandCombo\" was not injected: check your FXML file 'MainScreen.fxml'.")
  }

  protected def showPopupInfo(): Unit = {
    ViewUtilities.showNotificationPopup("Help", "Click '^' and create a configuration \nRight Click on screen hides toolbar",
      JavafxEnums.LONG_DURATION, JavafxEnums.INFO_NOTIFICATION, null)
  }

  private def initCombos(): Unit = {
    this.serverStateCombo.getSelectionModel.selectedItemProperty()
      .addListener((_, _, newValue) => log(newValue))
    this.serverCommandCombo.getSelectionModel.selectedItemProperty()
      .addListener((_, _, newValue) => log(newValue))
  }

  def addServersToMap(serverID: String): Unit = {
    //ID NODE
    var serverIDNode = new HBox()
    var labelIDNode = new Label(serverID)
    labelIDNode.setGraphic(ViewUtilities.iconSetter(Ionicons.ION_CUBE, JavafxEnums.BIGGER_ICON))
    serverIDNode.getChildren.add(labelIDNode)
    //LOG NODE
    var serverLogNode = new HBox()
    serverLogNode.setMinHeight(50)
    //ADDING
    var scrollPaneLogNode = new ScrollPane()
    scrollPaneLogNode.setMinHeight(JavafxEnums.BIGGER_ICON.dim)
    scrollPaneLogNode.setVbarPolicy(ScrollBarPolicy.NEVER)
    scrollPaneLogNode.setHbarPolicy(ScrollBarPolicy.NEVER)
    scrollPaneLogNode.setPannable(true)
    scrollPaneLogNode.setFitToHeight(true)
    scrollPaneLogNode.setContent(serverLogNode)
    this.vBoxServerNames.getChildren.add(serverIDNode)
    this.vBoxServerLogs.getChildren.add(scrollPaneLogNode)
    this.serverToHBox = this.serverToHBox + (serverID -> (serverIDNode, serverLogNode))
  }

  protected def addServerToCombos(serverID: String): Unit = {
    this.serverStateCombo.getItems.add(serverID)
    this.serverCommandCombo.getItems.add(serverID)
  }

  //protected def addEntryToLog(serverID: String, commando)
}
