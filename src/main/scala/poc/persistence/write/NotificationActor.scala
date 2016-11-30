package poc.persistence.write

import akka.actor._
import akka.persistence._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import scala._

trait StateNotification
case object READ extends StateNotification
case object NO_READ extends StateNotification


case class CmdNotificaction(idMsg: Long, idNotification: String, iduser: String, state: StateNotification, text : String )
case class EventNotification( timeStamp: Long, order: CmdNotificaction )



class NotificationActor( val id : String ) extends PersistentActor with AtLeastOnceDelivery {
	
	override def persistenceId = id

	val receiveRecover: Receive = ???

	val receiveCommand: Receive = ???  

}


