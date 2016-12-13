package poc.persistence.write.commands

sealed trait Command

final case class InitializeOrder(idOrder: String, idUser: Long, orderData: String) extends Command

final case class CancelOrder(idOrder: String, idUser: Long) extends Command
