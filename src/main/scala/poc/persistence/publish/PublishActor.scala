package poc.persistence.publish

import akka.actor.SupervisorStrategy.Stop
import akka.cluster.sharding.ShardRegion
import akka.persistence._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.actor._
import poc.persistence.BaseObjectActor

import scala.language.postfixOps
import poc.persistence.functional.write.OrderEvent 
import poc.persistence.write.WithOrder   

case class AckOrderConfirmed( deliverId : Long, idOrder : String, version : Long ) extends WithOrder
case class AckOrder( deliverId : Long, idOrder : String, version : Long ) extends WithOrder
case class OrderEventWithDeliver( deliverId: Long, event : OrderEvent )
case class DeliverId( deliverId : Long )

object PublishActor extends BaseObjectActor {
  
  import poc.persistence.write.WithOrder

  protected def props( implicit system : ActorSystem  ) =  {
    
    Props( classOf[PublishActor] )
   
 }

  val name = "publisher"

  // the input for the extractShardId function
  // is the message that the "handler" receives
  protected def extractShardId: ShardRegion.ExtractShardId = {
    case msg: WithOrder =>
      (msg.idOrder.hashCode.abs % 2).toString
  }

  // the input for th extractEntityId function
  // is the message that the "handler" receives
  protected def extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: WithOrder =>
      (msg.idOrder, msg)
  }

}

class PublishActor 
  extends PersistentActor 
    with ActorLogging 
      with AtLeastOnceDelivery {

  import poc.persistence.functional.Poc

  import ShardRegion.Passivate

  import scala.concurrent.duration._
  import akka.actor.Status._
  
  val streamActor = Poc.streamActor
  
  context.setReceiveTimeout(120 seconds)

  // self.path.name is the entity identifier (utf-8 URL-encoded)
  override def persistenceId: String = s"publish-${self.path.name}" 


  val receiveCommand: Receive = {
    
    case o: OrderEvent =>
      log.info("Received orderEvent ! . I am {}", self.path)
      
      persist( o ) { e =>
        deliver( streamActor.path ) { deliverId => 
          
          log.info( "Persisted  event! deliverId -> {},  event -> {} ", deliverId, o )
          
          sender ! DeliverId( deliverId )
          
          OrderEventWithDeliver( deliverId , e )
        }
      }
      
    case AckOrder( deliverId, idOrder, version ) =>    
      log.info("Recived AckOrder. IdDeliver = {}. I am {}", deliverId, self.path )
      persist( AckOrderConfirmed( deliverId, idOrder, version ) ) {
          e => confirmDelivery( deliverId )
          sender ! Status.Success( true )
      }

    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop => context.stop(self)

  }

  def receiveRecover = {
    case e: OrderEvent =>
      log.info("Received an event I need to process for recovery")
      deliver( streamActor.path)( OrderEventWithDeliver( _ , e ) )
    case AckOrderConfirmed( deliverId, _, _ )  => confirmDelivery( deliverId )
    case msg => log.error("unknown message from recover {} ", msg )
    
  }

  //~ def onEvent(e: Event ) = {
    //~ log.info("Changing internal state in response to an event!")
    //~ e match {
      //~ case e: OrderInitialized =>
        //~ state = StateOrder.IN_PROGRESS
        //~ deliver( streamActor.path ) { deliverId => 
          //~ EnvelopeOrderWithAck( AckOrder( deliverId ), self, e )
        //~ }
      //~ case e: OrderCancelled =>
        //~ state = StateOrder.CANCELLED
        //~ deliver( streamActor.path ) { deliverId => 
          //~ EnvelopeOrderWithAck( AckOrder( deliverId ), self, e )
        //~ }
      //~ case AckConfirmed( timeStamp, deliverId) => 
        //~ confirmDelivery( deliverId ) 
        

    //~ }

  //~ }

}





