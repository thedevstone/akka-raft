package it.unibo.sd1920.akka_raft.view.screens

import com.jfoenix.controls.JFXComboBox
import it.unibo.sd1920.akka_raft.view.utilities.{JavafxEnums, ViewUtilities}
import javafx.fxml.FXML
import javafx.scene.layout.{BorderPane, VBox}

trait View {
  def log(message: String): Unit
}

abstract class AbstractMainScreenView extends View {

  @FXML protected var mainBorder: BorderPane = _
  @FXML protected var vBoxServerNames: VBox = _
  @FXML protected var vBoxServerLogs: VBox = _
  @FXML protected var serverStateCombo: JFXComboBox[String] = _
  @FXML protected var serverCommandCombo: JFXComboBox[String] = _

  @FXML def initialize(): Unit = {
    this.assertNodeInjected()
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

  protected def initServerCombos(serverIDs: List[String]): Unit = {
    serverIDs.foreach(this.serverStateCombo.getItems.add(_))
    serverIDs.foreach(this.serverCommandCombo.getItems.add(_))
    this.serverStateCombo.getSelectionModel.selectedItemProperty()
      .addListener((_, _, newValue) => log(newValue))
    this.serverCommandCombo.getSelectionModel.selectedItemProperty()
      .addListener((_, _, newValue) => log(newValue))

  }

  protected def createServerLogs(serverIDs: List[String]): Unit = {} //TODO
}
