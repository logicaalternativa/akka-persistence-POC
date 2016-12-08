package poc.persistence.write

import java.nio.charset.Charset

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.event.Logging
import akka.persistence._
import org.json4s.DefaultFormats
import poc.persistence.write.Commands.{CancelOrder, InitializeOrder}

import scala.language.postfixOps

sealed trait OrdState
package OrderState {


  case object NONE extends OrdState

  case object CANCELLED extends OrdState

  case object IN_PROGRESS extends OrdState

  case object COMPLETE extends OrdState

}

trait Command

package Commands {

  case class InitializeOrder(idOrder: String, idUser: Long, orderData: Map[String, String]) extends Command

  case class CancelOrder(idOrder: String, idUser: Long) extends Command

}

sealed trait Event

package Events {

  case class OrderInitialized(idOrder: String, idUser: Long, orderData: Map[String, String]) extends Event

  case class OrderCancelled(idOrder: String, idUser: Long) extends Event

}

object OrderActor {

    def props = Props(classOf[OrderActor])

    val name = "orders"

    // the input for the extractShardId function
    // is some message that the "handler" receives
    def extractShardId: ShardRegion.ExtractShardId = {
      case msg: InitializeOrder =>
        msg.idUser.toString
      case msg: CancelOrder =>
        msg.idUser.toString
    }

    // the input for the extractEntityId function
    // is some message that the "handler" receives
    def extractEntityId: ShardRegion.ExtractEntityId = {
      case msg: InitializeOrder =>
        (msg.idOrder.toString, msg)
      case msg: CancelOrder =>
        (msg.idOrder.toString, msg)
    }

}

class OrderActor extends PersistentActor with ActorLogging {

  import ShardRegion.Passivate

  import scala.concurrent.duration._
  context.setReceiveTimeout(120 seconds)

  override def persistenceId: String = self.path.name

 var state: OrdState = OrderState.NONE

  val receiveCommand: Receive = {
    case command: Commands.InitializeOrder =>
      log.info("Received InitializeOrder command!")
      if (state == OrderState.NONE) {
        persist(Events.OrderInitialized(command.idOrder, command.idUser, command.orderData)) { e =>
          onEvent(e)
          log.info("Persisted OrderInitialized event!")
        }
      } else {
        log.info("Command rejected!")
        sender ! "Cannot initialize order since it has already been initialized, cancelled or completed"
      }

    case command: Commands.CancelOrder =>
      if (state == OrderState.IN_PROGRESS) {
        log.info("Received CancelOrder command!")
        persist(Events.OrderCancelled(command.idOrder,command.idUser)) { e =>
          onEvent(e)
          log.info("Persisted OrderCancelled event!")
        }
      } else {
        // Sometimes you may want to persist an event: OrderCancellationRequestRejected
        log.info("Command rejected!")
        sender ! "Cannot cancel order if it is not in progress"
      }

    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Stop)
      log.info("Sleeping")
    case Stop => context.stop(self)

  }

  var numEvents = 0
  def receiveRecover = {
    case RecoveryCompleted =>
      log.info("Recovery completed. Replayed {} events!", numEvents)
      case e: Event =>
        numEvents = numEvents + 1
        onEvent(e)
      case _ =>
  }

  def onEvent(e: Event) = {
    e match {
      case e: Events.OrderInitialized =>
        state = OrderState.IN_PROGRESS
      case e: Events.OrderCancelled =>
        state = OrderState.CANCELLED
    }
  }

}

import akka.persistence.journal.{Tagged, WriteEventAdapter}

class OrderTaggingEventAdapter(actorSystem: ExtendedActorSystem) extends WriteEventAdapter {

  private val log = Logging.getLogger(actorSystem, this)

  override def toJournal(event: Any): Any = event match {
    case e: Events.OrderInitialized =>
      log.debug("tagging OrderInitialized event")
      Tagged(e, Set("UserEvent"))
    case e: Events.OrderCancelled =>
      log.debug("tagged OrderCancelled event")
      Tagged(e, Set("UserEvent"))
  }

  override def manifest(event: Any): String = ""
}



import akka.serialization.Serializer

class EventSerialization extends Serializer {

  import org.json4s.native.Serialization.{read, write}

  val UTF8: Charset = Charset.forName("UTF-8")

  implicit val formats = DefaultFormats

  // Completely unique value to identify this implementation of Serializer, used to optimize network traffic.
  // Values from 0 to 16 are reserved for Akka internal usage.
  // Make sure this does not conflict with any other kind of serializer or you will have problems
  override def identifier: Int = 90020001

  override def includeManifest = true

  override def fromBinary(bytes: Array[Byte], manifestOpt: Option[Class[_]]): AnyRef = {
    implicit val manifest = manifestOpt match {
      case Some(x) => Manifest.classType(x)
      case None => Manifest.AnyRef
    }
    val result = read(new String(bytes, UTF8))
    result
  }

  override def toBinary(o: AnyRef): Array[Byte] = write(o).getBytes(UTF8)
}

