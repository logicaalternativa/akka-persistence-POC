package poc.persistence
package functional
package publish


object PublishServiceActor {
  
  import akka.actor.{ActorRef, ActorSystem}
  import Poc.ProgramAsync
  import poc.persistence.publish.PublishActor
  import scala.concurrent.{Future, ExecutionContext}
  
  
  def apply(implicit ec : ExecutionContext, system: ActorSystem ) = new PublishService[ProgramAsync] {
    
    import poc.persistence.functional.write.OrderEvent
    import poc.persistence.functional.{Error,ErrorActorOrder}
    import poc.persistence.functional.Poc.ProgramAsync
    import scalaz.{MonadError, MonadReader}
    import scalaz.syntax.monadError._
    import scalaz.syntax._
    import scalaz.{\/-, -\/} 
    import akka.pattern.Patterns
    import poc.persistence.publish.DeliverId
    
    val publishActor = PublishActor.receiver 
    
    implicit val E: MonadError[ProgramAsync,Error] = Poc.createMonadError

    import scala.concurrent.duration._
    
    /**
     * 
     * */
    def publish(orderEvent: OrderEvent): ProgramAsync[Long] = {
      
      val res0 = Patterns ask ( publishActor, orderEvent, 20 seconds )
      
      val res1 : Future[Long] = mapToDeliverId( res0 )
      
      mapToP( res1, orderEvent.idOrder )
      
     
    }
    
    /**
     * 
     * */
    def sendAck(deliverId: Long, idOrder : String, version : Long): ProgramAsync[Boolean] = {
      
      import poc.persistence.publish.AckOrder
      
      
      val res0 : Future[Any] = Patterns ask ( publishActor, AckOrder( deliverId, idOrder, version ), 20 seconds )
      
      val res1 = res0 map {
          _ match {
              case msg : Boolean => msg
              case msg           =>  { 
                throw new RuntimeException( s"Unknow message $msg" )
              }
          }
      }
      
      mapToP( res1, idOrder )
      
    }
    
    private def mapToP[A] ( from : Future[A], idOrder : String ) : ProgramAsync[A] = {
      
      from flatMap{ 
        _.point 
      } recoverWith{
        case e  =>  (ErrorActorOrder( idOrder, e ): Error).raiseError
      }
      
    }

    /**
     * 
     * */
    private def mapToDeliverId( res0 : Future[Any] )(implicit ec: ExecutionContext) : Future[ Long ] = {
  
      res0 map {
          _ match {
              case DeliverId( deliveryId ) => deliveryId
              case msg                     =>  throw new RuntimeException( s"Unknow message $msg" )
          }
      }
  
    }
    
  }
      
    
  
    
    
}




