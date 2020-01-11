package it.unibo.sd1920.akka_raft.view.utilities

import eu.hansolo.enzo.canvasled.Led


sealed case class LedPatch(name: String) extends Led() {
  override protected def getUserAgentStylesheet: String = getClass.getResource("/sheets/led.css").toExternalForm
  this.setId("patch-led")
  this.setFrameVisible(false)
}
sealed case class LedGuardian(name: String) extends Led() {
  override protected def getUserAgentStylesheet: String = getClass.getResource("/sheets/led.css").toExternalForm
  this.setId("guardian-led")
  this.setFrameVisible(false)
}