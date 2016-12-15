package poc.persistence.events

sealed trait OrderEvent

case class OrderCancelled(idOrder: String, idUser: Long) extends OrderEvent

case class OrderInitialized(idOrder: String, idUser: Long) extends OrderEvent
