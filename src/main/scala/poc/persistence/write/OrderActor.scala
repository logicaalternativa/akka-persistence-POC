package poc.persistence.write

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.persistence._

import scala.language.postfixOps

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
  val idUser: Long
}

package Commands {

  case class InitializeOrder(idMsg: Long, idOrder: String, idUser: Long) extends WithUser with WithOrder

  case class CancelOrder(idMsg: Long, idOrder: String, idUser: Long) extends WithUser with WithOrder

}

sealed trait Event

package Events {

  case class OrderInitialized(timeStamp: Long, order: Commands.InitializeOrder) extends Event

  case class OrderCancelled(timeStamp: Long, order: Commands.CancelOrder) extends Event

}

object OrderActor {

    def props = Props(classOf[OrderActor])

    val name = "orders"

    def extractShardId: ShardRegion.ExtractShardId = {
      case msg: WithUser =>
        (msg.idUser % 2).toString
    }

    def extractEntityId: ShardRegion.ExtractEntityId = {
      case msg: WithOrder =>
        (msg.idOrder, msg)
    }

}

class OrderActor extends PersistentActor with ActorLogging with AtLeastOnceDelivery {

  import ShardRegion.Passivate

  import scala.concurrent.duration._
  context.setReceiveTimeout(120 seconds)

  // self.path.name is the entity identifier (utf-8 URL-encoded)
  override def persistenceId: String = "Counter-" + self.path.name


  val receiveCommand: Receive = {
    case o: Commands.InitializeOrder =>
      log.info("Received InitializeOrder command!")
      persist(Events.OrderInitialized(System.nanoTime(), o)) { e =>
        onEvent(e)
        log.info("Persisted OrderInitialized event!")
      }

    case o: Commands.CancelOrder =>
      if (state == StateOrder.IN_PROGRESS) {
        log.info("Received CancelOrder command!")
        persist(Events.OrderCancelled(System.nanoTime(), o)) { e =>
          onEvent(e)
          log.info("Persisted OrderCancelled event!")
        }

      } else {
        // Sometimes you may want to persist an event OrderCancellationRequestRejected
        log.info("Command rejected!")
        sender ! "Cannot cancel order if it is not in progress"
      }

    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop => context.stop(self)

  }
  var state: StateOrder = StateOrder.NONE

  def receiveRecover = {
    case e: Event =>
      log.info("Received an event I need to process for recovery")
      onEvent(e)
    case _ =>
  }

  def onEvent(e: Event) = {
    log.info("Changing internal state in response to an event!")
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
      Tagged(e, Set(e.order.idUser.toString))
    case e: Events.OrderCancelled =>
      Tagged(e, Set(e.order.idUser.toString))
  }

  override def manifest(event: Any): String = ""
}






