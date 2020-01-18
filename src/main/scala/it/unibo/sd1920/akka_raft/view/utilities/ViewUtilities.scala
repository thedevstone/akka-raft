package it.unibo.sd1920.akka_raft.view.utilities

import com.jfoenix.controls.{JFXDialog, JFXDialogLayout}
import eu.hansolo.enzo.notification.{Notification, NotificationEvent}
import it.unibo.sd1920.akka_raft.view.FXMLScreens.{FXMLScreens, HOME}
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.{CacheHint, Node, Scene}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import javafx.util.Duration
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.javafx.FontIcon

private[view] object ViewUtilities {
  def loadFxml(controller: Any, fxml: FXMLScreens): Node = {
    val loader = new FXMLLoader(ViewUtilities.getClass.getResource(fxml.resourcePath))
    loader.setController(controller)
    loader.load()
  }

  def chargeSceneSheets(scene: Scene): Unit = {
    scene.getStylesheets.add(ViewUtilities.getClass.getResource(HOME.cssPath).toString)
  }

  /**
   * A simple method for charging ikon in node.
   *
   * @param icon
   * the { @link Ikon}
   * @param fontSize
   * the font size
   * @return { @link IconDimension} the { @link IconDimension}
   */
  def iconSetter(icon: Ikon, fontSize: JavafxEnums.IconDimension): FontIcon = {
    val tempIcon = new FontIcon(icon)
    tempIcon.setIconSize(fontSize.dim)
    tempIcon
  }

  /**
   * Show a dialog into the main pane.
   *
   * @param mainPane
   * the main { @link StackPane}
   * @param title
   * the String title dialog
   * @param description
   * the STring description
   * @param size
   * the  size
   * @param ev
   * the { @link MouseEvent}
   */
  def showDialog(mainPane: StackPane, title: String, description: String, size: JavafxEnums.DimDialog, ev: EventHandler[MouseEvent]): Unit = {
    var css = ""
    val content = new JFXDialogLayout
    val titolo = new Text(title)
    val descrizione = new Text(description)
    size match {
      case JavafxEnums.SMALL_DIALOG =>
        css = "dialogTextSmall"
      case JavafxEnums.MEDIUM_DIALOG =>
        css = "dialogTextMedium"
      case JavafxEnums.BIG_DIALOG =>
        css = "dialogTextBig"
      case _ =>
    }
    descrizione.getStyleClass.add(css)
    titolo.getStyleClass.add(css)
    content.setHeading(titolo)
    content.setBody(descrizione)
    content.getStyleClass.add("dialogContentBackground")
    val dialog = new JFXDialog(mainPane, content, JFXDialog.DialogTransition.CENTER)
    dialog.getStyleClass.add("dialogBackground")
    dialog.show()
    content.setCache(true)
    content.setCacheHint(CacheHint.SPEED)
    dialog.setOnMouseClicked(ev)
  }

  /**
   * Show a notification popup into the main windows of the operating system.
   *
   * @param title
   * the String title of the { @link Notification}
   * @param message
   * the String text of the { @link Notification}
   * @param secondsDuration
   * the number of  of the
   * { @link Notification}
   * @param notiType
   * the { @link NotificationType} of the { @link Notification}
   * @param ev
   * the { @link EventHandler} ev, lalmba
   */
  def showNotificationPopup(title: String, message: String, secondsDuration: JavafxEnums.Notification_Duration, notiType: JavafxEnums.NotificationType, ev: EventHandler[NotificationEvent]): Unit = { // _____________________________PATTERN STRATEGY
    val no = Notification.Notifier.INSTANCE
    val notification = new Notification(title, message)
    no.setPopupLifetime(Duration.seconds(secondsDuration.time))
    notiType match {
      case JavafxEnums.ERROR_NOTIFICATION =>
        no.notifyError(title, message)
      case JavafxEnums.WARNING_NOTIFICATION =>
        no.notifyWarning(title, message)
      case JavafxEnums.SUCCESS_NOTIFICATION =>
        no.notifySuccess(title, message)
      case JavafxEnums.INFO_NOTIFICATION =>
        no.notifyInfo(title, message)
      case _ =>
        no.notify(notification)
    }
    no.setOnNotificationPressed(ev)
  }
}
