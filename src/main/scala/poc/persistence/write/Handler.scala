package poc.persistence.write

import akka.actor._
import akka.persistence._
import poc.persistence.write._
import scala._


class Handler extends Actor {
	
	def getOrderChild(id: String) = context.child(id).getOrElse(OrderActor.props(id))
	def getNotificationChild(id:String) = context.child(msg.idOrder).getOrElse(NotificationActor.props(msg.idOrder))
		
	def receive : Receive = {
		
		case msg : CmdOrder => {
		 getOrderChild(msg.idOrder) ! msg
		}
		
		case msg : CmdNotificaction => {
		 getNotificationChild(msg.idNotification)
		}
		
	}

	
}


