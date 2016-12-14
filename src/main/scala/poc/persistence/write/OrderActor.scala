package poc.persistence.write

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.persistence._
import poc.persistence.events.{Event, OrderCancelled, OrderInitialized}
import poc.persistence.write.OrderState.OrdState
import poc.persistence.write.commands.{CancelOrder, InitializeOrder}

import scala.language.postfixOps

package OrderState {

  sealed trait OrdState

  case object NONE extends OrdState

  case object CANCELLED extends OrdState

  case object IN_PROGRESS extends OrdState

  case object COMPLETE extends OrdState

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
    case command: commands.InitializeOrder =>
      log.info("Received InitializeOrder command!")
      if (state == OrderState.NONE) {
        persist(OrderInitialized(command.idOrder, command.idUser)) { e =>
          onEvent(e)
          log.info("Persisted OrderInitialized event!")
        }
        sender ! 'CommandAccepted
      } else {
        log.info("Command rejected!")
        sender ! 'CommandRejected
      }

    case command: commands.CancelOrder =>
      if (state == OrderState.IN_PROGRESS) {
        log.info("Received CancelOrder command!")
        persist(OrderCancelled(command.idOrder,command.idUser)) { e =>
          onEvent(e)
          log.info("Persisted OrderCancelled event!")
        }
        sender ! 'CommandAccepted
      } else {
        // Sometimes you may want to persist an event: OrderCancellationRequestRejected
        log.info("Command rejected!")
        sender ! 'CommandRejected
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
      case e: OrderInitialized =>
        state = OrderState.IN_PROGRESS
      case e: OrderCancelled =>
        state = OrderState.CANCELLED
    }
  }

}

