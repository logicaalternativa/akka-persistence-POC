package poc.persistence.write

import akka.actor._
import akka.persistence._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import scala._

trait StateOrder
case object INIT extends StateOrder
case object OK extends StateOrder
case object CANCELLED extends StateOrder
case object IN_PROGRESS extends StateOrder


object Commands {
  case class InitializeOrder(idMsg: Long, idOrder: String, iduser: String) 
  case class CancelOrder(idMsg: Long, idOrder: String, iduser: String) 
}

object Events {
 case class OrderInitialized( timeStamp: Long, order: CmdOrder  )
 case class OrderCancelled( timeStamp: Long, order: CancelOrder  )
}


object OrderActor {
 
 def props(idOrder : String) = Props(classOf[OrderActor], idOrder)


}

class OrderActor(id : String ) extends PersistentActor with AtLeastOnceDelivery {
	
	var state = _ 
	
	override def persistenceId = id

	val receiveRecover: Receive = ??

	val receiveCommand: Receive = {
		case o: Commands.InitializeOrder =>
		  persist(Events.OrderInitialized( System.nanoTime(), o)) { e=>
			  onEvent(e)
		  }
		case o: Commands.CancelOrder =>
		   if (state = State.IN_PROGRESS)  {
			   persist(Events.OrderCancelled) { e => 
	 			   onEvent(e)
	   	}
			   
		   } else {
			   // It could persist an event
			   // For example, for a user login system the event can be UserAuthFailed  
			   sender ! NotAccepted("Cannot cancel order if it is not in progress")
		   }
		
	}
	
	def onEvent = {
		case OrderInitialized =>
		  state = State.IN_PROGRESS
		case OrderCancelled => 
		  state = State.CANCELLED  
	}
	
	def receiveRecover = {
		case e => onEvent(e)
	}
	

}






import akka.persistence.journal.WriteEventAdapter
import akka.persistence.journal.Tagged
 
 
 
 
class OrderTaggingEventAdapter extends WriteEventAdapter {
	
  override def toJournal(event: Any): Any = event match {
	  case e: EventOrder => Tagged(e, Set(e.order.idUser))
	  case _ => 
  }
 
  override def manifest(event: Any): String = ""
}






