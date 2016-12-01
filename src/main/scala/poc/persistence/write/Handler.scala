package poc.persistence.write

import akka.actor._
import akka.persistence._
import poc.persistence.write._
import scala._


class Handler extends Actor with ActorLogging {
	
	def getOrderChild(id: String): ActorRef = context.child(id).getOrElse(context.actorOf(OrderActor.props(id), id))
	//~ def getNotificationChild(id:String): ActorRef = context.child(id).getOrElse(context.actorOf(NotificationActor.props(id)))
		
	def receive : Receive = {
		case msg : WithOrder => {
			 getOrderChild(msg.idOrder) ! msg
			 log.info("message with idOrder = {}", msg)
		}
		case s: String => 
			log.info(s)
	}

	
}


