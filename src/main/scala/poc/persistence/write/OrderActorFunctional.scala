package poc.persistence
package write

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.persistence._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import poc.persistence.BaseObjectActor

import scala.language.postfixOps

import poc.persistence.functional.write.{InitOrder,CloseOrder, CancelOrder}

sealed trait Query

package Querys{
  
  case class GetLastEvent(idOrder : String) extends Query with WithOrder with Serializable
  
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



object OrderActorFunctional extends BaseObjectActor {
  
  import akka.stream.Materializer

  protected def props( implicit system : ActorSystem  ) =  {
    
    Props(classOf[OrderActorFunctional] )
   
 }

  val name = "orders"

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

class OrderActorFunctional 
  extends PersistentActor 
    with ActorLogging 
      with AtLeastOnceDelivery {

  import ShardRegion.Passivate
  
  import poc.persistence.functional.write._

  import scala.concurrent.duration._
  import akka.actor.Status._
  import poc.persistence.functional.{OrderVersionNoOk,OrderNotYetInitialized}
  
  var orderEvent : Option[OrderEvent]= Option.empty 
   
  
  context.setReceiveTimeout(120 seconds)

  // self.path.name is the entity identifier (utf-8 URL-encoded)
  override def persistenceId: String = s"orders-${self.path.name}" 


  val receiveCommand: Receive = {
    
    //~ import Status._
    
    case Querys.GetLastEvent( idOrder ) => {
      
      orderEvent match {
                  case Some( orderEvent ) => sender ! orderEvent
                  case None               => sender ! Status.Failure( OrderNotYetInitialized( idOrder ) )
                }
    }
    
    case o: OrderEvent => { 
      
      val lastVersion = orderEvent.fold( -1L )( _.version )
      
      if ( lastVersion >= o.version ) {
        sender ! Failure( OrderVersionNoOk( o, lastVersion ) )
      } else {
       
        persist( o ) { e =>
          onEvent(e)
          log.info("Persisted OrderInitialized event! ->{}", e )
          sender ! e
        }
      }
    }
    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop => context.stop(self)
    case msg =>  {
          log.info( "Unknow message receiveCommand -> {}", msg )
          unhandled( msg )
        }
  }
  

  def receiveRecover = {
    
    case e : OrderEvent => onEvent( e )
    case msg => log.info( "Unknow message receiveRecover ->  {}", msg )
    
  }
  
  def onEvent(e : OrderEvent ) = {
    
    orderEvent = Option( e ) 
    
  }

  
}








