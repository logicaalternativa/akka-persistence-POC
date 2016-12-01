package poc.persistence.write

import akka.actor._
import akka.persistence._

sealed trait StateOrder

object StateOrder {

  case object NONE extends StateOrder

  case object INIT extends StateOrder

  case object OK extends StateOrder

  case object CANCELLED extends StateOrder

  case object IN_PROGRESS extends StateOrder

}

trait WithOrder {
  val idOrder: String
}

trait WithUser {
  val idUser: String
}

object Commands {

  case class InitializeOrder(idMsg: Long, idOrder: String, idUser: String) extends WithUser with WithOrder

  case class CancelOrder(idMsg: Long, idOrder: String, idUser: String) extends WithUser with WithOrder

}


sealed trait Event
package Events {

  case class OrderInitialized(timeStamp: Long, order: Commands.InitializeOrder) extends Event

  case class OrderCancelled(timeStamp: Long, order: Commands.CancelOrder) extends Event

}


object OrderActor {

  def props(idOrder: String) = Props(classOf[OrderActor], idOrder)

}

class OrderActor(id: String) extends PersistentActor with ActorLogging with AtLeastOnceDelivery {

  val receiveCommand: Receive = {
    case o: Commands.InitializeOrder =>
      log.info("Received InitializeOrder command! -> {}", o)
      persist(Events.OrderInitialized(System.nanoTime(), o)) { e =>
        onEvent(e)
        log.info("Persisted OrderInitialized event! -> {}", e)
      }

    case o: Commands.CancelOrder =>
      if (state == StateOrder.IN_PROGRESS) {
        log.info("Received CancelOrder command! -> {}", o)
        persist(Events.OrderCancelled(System.nanoTime(), o)) { e =>
          onEvent(e)
          log.info("Persisted OrderCancelled event! -> {}", e)
        }

      } else {
        // It could persist an event
        // For example, for a user login system the event can be UserAuthFailed
        log.info("Command rejected!")
        sender ! "Cannot cancel order if it is not in progress"
      }

  }
  var state: StateOrder = StateOrder.NONE

  override def persistenceId = id

  def receiveRecover = {
    case e: Event =>
      log.info("Receiving event I need to process")
      onEvent(e)
    case _ =>
  }

  def onEvent(e: Event) = {
    log.info("Changing internal state in response to an event! {}", e)
    e match {
      case e: Events.OrderInitialized =>
        state = StateOrder.IN_PROGRESS
      case e: Events.OrderCancelled =>
        state = StateOrder.CANCELLED
    }
  }

}


import akka.persistence.journal.{Tagged, WriteEventAdapter}


class OrderTaggingEventAdapter extends WriteEventAdapter {

  override def toJournal(event: Any): Any = event match {
    case e: Events.OrderInitialized =>
      Tagged(e, Set(e.order.idUser))
    case e: Events.OrderCancelled =>
      Tagged(e, Set(e.order.idUser))
  }

  override def manifest(event: Any): String = ""
}






