package poc.persistence.functional
package write

import poc.persistence.write.{WithOrder,WithUser}
import poc.persistence.functional.{Error => Error}

// Commands
sealed trait OrderCommand { 
  val idOrder: String
  val idUser: Long 
}

case class InitOrder( idOrder: String, idUser: Long ) extends OrderCommand
case class CloseOrder( idOrder: String, idUser: Long ) extends OrderCommand
case class CancelOrder( idOrder: String, idUser: Long ) extends OrderCommand

// Events
sealed trait OrderEvent { 
  val idOrder: String 
  val version: Long 
}
case class NoInitiatedOrder( idOrder: String, version : Long ) extends OrderEvent with WithOrder
case class InitiatedOrder( idOrder: String, idUser: Long, version : Long ) extends OrderEvent with WithOrder with WithUser
case class ClosedOrder( idOrder: String, idUser: Long, version : Long ) extends OrderEvent with WithOrder with WithUser
case class CanceledOrder( idOrder: String, idUser: Long, version : Long ) extends OrderEvent with WithOrder with WithUser






object OrderService {
  
  def apply[P[_]](implicit OrderService: OrderService[P]) = OrderService

  object Syntax{
    def getLastEvent[P[_]]( idOrder: String )( implicit orderSvc: OrderService[P] )  = orderSvc.getLastEvent( idOrder )
    def saveEventFromCommand[P[_]]( fromCommand : OrderCommand )( implicit orderSvc: OrderService[P] ) = orderSvc.saveEventFromCommand( fromCommand )
    def saveEvent[P[_]]( event : OrderEvent )( implicit orderSvc: OrderService[P] ) = orderSvc.saveEvent( event )
  }
  
}


trait OrderService [P[_]] {
  
  import scalaz.MonadError
  import scalaz.syntax.monadError._
  import scalaz.syntax._
  
  implicit val E: MonadError[P,Error]
  
  // Primitives
  def getLastEvent( idOrder: String ) : P[OrderEvent]
  
  def saveEvent( event : OrderEvent ) : P[OrderEvent]
  
  // Derived 
  def saveEventFromCommand( fromCommand : OrderCommand ) : P[OrderEvent] = {
      
      for {
        
        lastEvent <- getLastEvent( fromCommand.idOrder )  
        newEvent  <-  validateStateFromCommand( fromCommand, lastEvent ) >> 
                      getNextEvent( fromCommand, lastEvent.version + 1 )
        res <- saveEvent( newEvent )
        
      } yield( res )
     
  }
  
  private def getNextEvent( fromCommand : OrderCommand, newVersion : Long ) : P[OrderEvent] = {
    
    fromCommand match {
        case  InitOrder( idOrder, idUser )   =>  E pure ( InitiatedOrder( idOrder, idUser, newVersion ) )
        case  CloseOrder( idOrder, idUser )  =>  E pure ( ClosedOrder( idOrder, idUser, newVersion ) )
        case  CancelOrder( idOrder, idUser ) =>  E pure ( CanceledOrder( idOrder, idUser, newVersion ) )
      
    }
  }
  
  private def validateStateFromCommand( fromCommand : OrderCommand, toEvent : OrderEvent ) :P[Unit] = {
      
      toEvent match {
        
        case noInitiated    : NoInitiatedOrder => validateStateNoInitiatedOrder( fromCommand )( noInitiated )
        case initiatedOrder : InitiatedOrder   => validateStateInitiatedOrder( fromCommand )( initiatedOrder )
        case ClosedOrder( idOrder, _, _ )      => E raiseError OrderIsClosed( idOrder )
        case CanceledOrder( idOrder,_,_ )      => E raiseError OrderIsClosed( idOrder )
        
      }
      
  }
  
  private def validateStateNoInitiatedOrder( fromCommand : OrderCommand )( noInitiated: NoInitiatedOrder ) : P[Unit] = {
    
    fromCommand match {
        
      case initOrder : InitOrder => ().point
      case command   : OrderCommand => E raiseError OrderIsNoInitiated( command )
    
    }
    
  }
  
  private def validateStateInitiatedOrder( fromCommand : OrderCommand )( initiatedOrder : InitiatedOrder ) : P[Unit] = {
    
    fromCommand match {
      
      case initOrder  : CancelOrder => ().point
      case closeOrder : CloseOrder  => ().point
      case command : OrderCommand => E raiseError OrderIsInitiated( command )
    
    }
    
  }
  

}
