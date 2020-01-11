package it.unibo.sd1920.akka_raft.view.utilities

object JavafxEnums {
  sealed abstract class DimDialog(val dim: Int) {}
  case object BIG_DIALOG extends DimDialog(3)
  case object MEDIUM_DIALOG extends DimDialog(2)
  case object SMALL_DIALOG extends DimDialog(1)

  sealed abstract class IconDimension(val dim: Int) {}
  case object BIGGEST_ICON extends IconDimension(80)
  case object BIGGER_ICON extends IconDimension(60)
  case object BIG_ICON extends IconDimension(40)
  case object MEDIUM_ICON extends IconDimension(30)
  case object SMALL_ICON extends IconDimension(20)
  case object SMALLER_ICON extends IconDimension(15)
  case object SMALLEST_ICON extends IconDimension(10)

  sealed abstract class NotificationType(val code: Int) {}
  case object ERROR_NOTIFICATION extends NotificationType(1)
  case object WARNING_NOTIFICATION extends NotificationType(2)
  case object SUCCESS_NOTIFICATION extends NotificationType(3)
  case object INFO_NOTIFICATION extends NotificationType(4)

  sealed abstract class Notification_Duration(val time: Int) {}
  case object LONG_DURATION extends Notification_Duration(7)
  case object MEDIUM_DURATION extends Notification_Duration(5)
  case object SHORT_DURATION extends Notification_Duration(3)

  object ShapeType extends Enumeration {
    val CIRCLE, SQUARE = Value
  }
}