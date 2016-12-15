package poc.persistence.read.user.events

sealed trait UserEvent
case class UserInitializedOrder(idOrder: String, idUser: Long) extends UserEvent
case class UserCancelledOrder(idOrder: String, idUser: Long) extends UserEvent

