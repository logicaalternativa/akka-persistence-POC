package poc.persistence.events

sealed trait Event

case class OrderCancelled(idOrder: String, idUser: Long) extends Event

case class OrderInitialized(idOrder: String, idUser: Long) extends Event