package poc.persistence
package functional
package write


object OrderServiceActor {
  
  import akka.actor.{ActorRef, ActorSystem}
  import scala.concurrent.{Future, ExecutionContext}
  import Poc.ProgramAsync
  
  def apply()(implicit ec : ExecutionContext, system : ActorSystem ) = new OrderService[ProgramAsync] {
    
    import scalaz.{MonadError, MonadReader}
    import scalaz.syntax.monadError._
    import scalaz.syntax._
    import scalaz.{\/-, -\/} 
    import scalaz.\/, \/.{left, right}
    import akka.pattern.Patterns
    import poc.persistence.write.OrderActorFunctional
    
    val actorOrder =  OrderActorFunctional.receiver
    
    implicit val E: MonadError[ProgramAsync,Error] = Poc.createMonadError

    import scala.concurrent.duration._
    
    // Primitives
    def getLastEvent( idOrder: String ) : ProgramAsync[OrderEvent] = {
      
      import poc.persistence.write.Querys._
      val res0 = Patterns ask ( actorOrder, GetLastEvent(idOrder), 20 seconds )
      
      val res : Future[OrderEvent] = mapToOrderEvent( res0 )
      
      res flatMap{ 
        _.point 
      } recoverWith{
        case OrderNotYetInitialized( idOrder ) =>  E point (NoInitiatedOrder( idOrder, 0) )
        case e                                 =>  (ErrorActorOrder( idOrder, e ): Error).raiseError
      }
      
    }
    
    /**
     * 
     **/
    def saveEvent( event : OrderEvent ): ProgramAsync[OrderEvent] = {
      
      val res0 = Patterns ask ( actorOrder, event, 30 seconds )
      
      val res : Future[OrderEvent] = mapToOrderEvent( res0 )
      
      res flatMap{ 
        _.point 
      } recoverWith {
        case error : OrderVersionNoOk =>  (error: Error).raiseError
        case e                        =>  ( ErrorActorOrder( event.idOrder, e ): Error).raiseError
      }
      
    }

  }
  
  private def mapToOrderEvent( res0 : Future[Any] )(implicit ec: ExecutionContext) : Future[OrderEvent] = {
    
      res0 map {
          _ match {
              case s : OrderEvent => s
              case msg            =>  throw new RuntimeException( s"Unknow message $msg" )
          }
      }
    
  }
    
    
}




