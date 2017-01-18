package poc.persistence.write

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.persistence._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import poc.persistence.BaseObjectActor

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

sealed trait Event {
  val timeStamp: Long
}

sealed trait EventOrder extends Event {
}

package Commands {

  case class InitializeOrder(idMsg: Long, idOrder: String, idUser: Long) extends WithUser with WithOrder

  case class CancelOrder(idMsg: Long, idOrder: String, idUser: Long) extends WithUser with WithOrder
  
  case class AckOrder( idDeliver : Long )
  
  case class EnvelopeOrderWithAck( ack : AckOrder, fromActor: ActorRef, event : EventOrder )

}


package Events {

  case class OrderInitialized(timeStamp: Long, order: Commands.InitializeOrder) extends EventOrder

  case class OrderCancelled(timeStamp: Long, order: Commands.CancelOrder) extends EventOrder
  
  case class AckConfirmed( timeStamp: Long, idDeliver : Long ) extends Event

}

object OrderActor extends BaseObjectActor {
  
  import akka.stream.Materializer

  protected def props( implicit system : ActorSystem  ) =  {
    
    import poc.persistence.stream.StreamToQuery._
  
    Props(classOf[OrderActor], actorStream )
   
 }

  val name = "orders"

  // the input for the extractShardId function
  // is the message that the "handler" receives
  protected def extractShardId: ShardRegion.ExtractShardId = {
    case msg: WithUser =>
      (msg.idUser % 2).toString
  }

  // the input for th extractEntityId function
  // is the message that the "handler" receives
  protected def extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: WithOrder =>
      (msg.idOrder, msg)
  }

}

class OrderActor( streamActor : ActorRef ) 
  extends PersistentActor 
    with ActorLogging 
      with AtLeastOnceDelivery {

  import ShardRegion.Passivate

  import scala.concurrent.duration._
  import akka.actor.Status._
  import Commands._
  import Events._    
  
  context.setReceiveTimeout(120 seconds)

  // self.path.name is the entity identifier (utf-8 URL-encoded)
  override def persistenceId: String = self.path.name


  val receiveCommand: Receive = {
    
    case o: InitializeOrder =>
      log.info("Received InitializeOrder command! . I am {}", self.path)
      persist( OrderInitialized(System.nanoTime(), o)) { e =>
        onEvent(e)
        log.info("Persisted OrderInitialized event!")
        sender ! Success( "Sucessfully persisted OrderInitialized")
      }
    
    case AckOrder( idDeliver ) =>    
      log.info("Recived AckOrder. IdDeliver = {}. I am {}", idDeliver, self.path )
      persist( AckConfirmed( System.nanoTime(), idDeliver ) ) {
          e => onEvent( e )
      }

    case o: Commands.CancelOrder =>
      if (state == StateOrder.IN_PROGRESS) {
        log.info("Received CancelOrder command!. I am {}", self.path)
        persist( OrderCancelled(System.nanoTime(), o) ) { e =>
          onEvent(e)
          log.info("Persisted OrderCancelled event!")
          sender ! Success( "Sucessfully persisted OrderCancelled")
        }

      } else {
        // Sometimes you may want to persist an event OrderCancellationRequestRejected
        log.info("Command rejected!")
        sender ! Failure( new Exception( "Cannot cancel order if it is not in progress") )
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

  def onEvent(e: Event ) = {
    log.info("Changing internal state in response to an event!")
    e match {
      case e: OrderInitialized =>
        state = StateOrder.IN_PROGRESS
        deliver( streamActor.path ) { deliverId => 
          EnvelopeOrderWithAck( AckOrder( deliverId ), self, e )
        }
      case e: OrderCancelled =>
        state = StateOrder.CANCELLED
        deliver( streamActor.path ) { deliverId => 
          EnvelopeOrderWithAck( AckOrder( deliverId ), self, e )
        }
      case AckConfirmed( timeStamp, idDeliver) => 
        confirmDelivery( idDeliver ) 
    }
  }

}

import akka.persistence.journal.{Tagged, WriteEventAdapter}

class OrderTaggingEventAdapter extends WriteEventAdapter {
  
  override def toJournal(event: Any): Any = event match {
    case e: Events.OrderInitialized =>
      //~ println("########## Event Adapter Works ############")
      Tagged(e, Set("byUser"))
    case e: Events.OrderCancelled =>
      //~ println("########## Event Adapter Works ############")
      Tagged(e, Set("byUser"))
  }

  override def manifest(event: Any): String = ""
}






