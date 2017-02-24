package poc.persistence
package functional

import akka.actor._
import akka.pattern.ask

import scala.util.{Success, Failure}
import poc.persistence.functional.write.{OrderCommand, InitiatedOrder}

object ApplicationFlow {  
  
  import scalaz.{Monad,MonadError}
  import poc.persistence.functional.write.OrderEvent
  import poc.persistence.functional.write.OrderService
  import poc.persistence.functional.publish.PublishService
  import poc.persistence.functional.read.UserViewService
  import poc.persistence.publish.OrderEventWithDeliver
  import scalaz.syntax.monadError._
  import PublishService.Syntax._
  
  def write[ P[_] ] ( command : OrderCommand )
      ( implicit 
          OS: OrderService[P],
          PS : PublishService[P],
          M : Monad[P],
          E : MonadError[P,Error]
      ) 
      : P[OrderEvent] = {
        
    import OrderService.Syntax._
    
    for {
      event <- saveEventFromCommand( command ) 
      _     <-  publish( event )
      
    } yield( event )
    
  } 
  
  def toView[ P[_] ] ( orderWithDeliver : OrderEventWithDeliver )
      ( implicit 
          PS: PublishService[P],
          US: UserViewService[P],
          M : Monad[P],
          E : MonadError[P,Error]
      ) 
      : P[Boolean] = {
        
    import UserViewService.Syntax._
    
    val event = orderWithDeliver.event
    
    update( event ) >> 
    sendAck( orderWithDeliver.deliverId, event.idOrder, event.version )

  }
  
}


