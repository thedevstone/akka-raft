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

  @FXML def initialize(): Unit = {
    this.assertNodeInjected()
    this.prepareCombos()
  }

  private def assertNodeInjected(): Unit = {
    assert(mainBorder != null, "fx:id=\"mainBorder\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(vBoxServerNames != null, "fx:id=\"vBoxServerNames\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(vBoxServerLogs != null, "fx:id=\"vBoxServerLogs\" was not injected: check your FXML file 'MainScreen.fxml'.")
  }

  private def showPopupInfo(): Unit = {
    ViewUtilities.showNotificationPopup("Help", "Click '^' and create a configuration \nRight Click on screen hides toolbar",
      JavafxEnums.LONG_DURATION, JavafxEnums.INFO_NOTIFICATION, null)
  }

  private def prepareCombos(): Unit = {
    /*
    ShapeType.values.foreach(this.comboBoxShape.getItems.add(_))
    this.comboBoxShape.getSelectionModel.selectedItemProperty()
      .addListener((_, _, newValue) => newValue match {
        case ShapeType.CIRCLE =>
        case ShapeType.SQUARE => shapeDrawingConsumer = squareDrawing
      })
     */
  }

  override def log(message: String): Unit

}
