package poc.persistence.write.functional

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
  val idUser: Long 
  val version: Long 
}
case class NoInitiatedOrder( idOrder: String, idUser: Long, version : Long ) extends OrderEvent
case class InitiatedOrder( idOrder: String, idUser: Long, version : Long ) extends OrderEvent
case class ClosedOrder( idOrder: String, idUser: Long, version : Long ) extends OrderEvent
case class CanceledOrder( idOrder: String, idUser: Long, version : Long ) extends OrderEvent

// Errors
sealed abstract class Error extends Throwable
case class OrderIsClosed( idOrder : String ) extends Error
case class OrderIsCanceled( idOrder : String ) extends Error
case class OrderIsNoInitiated( command : OrderCommand ) extends Error
case class OrderIsInitiated( command : OrderCommand ) extends Error
case class VersionOutOfDate( idOrder : String, version : Long ) extends Error

object OrderService {
  
  def apply[P[_]](implicit OrderService: OrderService[P]) = OrderService

    object Syntax{
      def getLastEvent[P[_]]( idOrder: String )(implicit orderSvc: OrderService[P])  = orderSvc.getLastEvent( idOrder )
      def validateOrder[P[_]]( fromCommand : OrderCommand )( implicit orderSvc: OrderService[P] ) = orderSvc.validateOrder( fromCommand )
      def saveCommand[P[_]]( command : OrderCommand )( version : Long )( implicit orderSvc: OrderService[P] ) = orderSvc.saveCommand( command )( version )
    }
  
}


trait OrderService [P[_]] {
  
  import scalaz.MonadError
  import scalaz.syntax.monadError._
  import scalaz.syntax._
  
  implicit val E: MonadError[P,Error]
  
  // Primitives
  def getLastEvent( idOrder: String ) : P[OrderEvent]
  
  def saveCommand( command : OrderCommand )( version : Long ) : P[OrderEvent]
  
  // Derived 
  def validateOrder( fromCommand : OrderCommand ) : P[OrderEvent] = {
      
      //~ getLastEvent( fromCommand.idOrder )  >>!  { 
        //~ event => validateStateFromCommand( fromCommand, event ) 
      //~ }
      //~ 
      for {
          
        lastEvent <- getLastEvent( fromCommand.idOrder )        
        newEvent  <- validateStateFromCommand( fromCommand, lastEvent )
        
      } yield( newEvent )
    
  }
  
  private def validateStateFromCommand( fromCommand : OrderCommand, toEvent : OrderEvent ) :P[OrderEvent] = {
      
      toEvent match {
        
        case noInitiated    : NoInitiatedOrder => validateStateNoInitiatedOrder( fromCommand )( noInitiated )
        case initiatedOrder : InitiatedOrder   => validateStateInitiatedOrder( fromCommand )( initiatedOrder )
        case ClosedOrder( idOrder, _, _ )   => E raiseError OrderIsClosed( idOrder )
        case CanceledOrder( idOrder,_,_ ) => E raiseError OrderIsClosed( idOrder )
        
      }
      
  }
  
  private def validateStateNoInitiatedOrder( fromCommand : OrderCommand )( noInitiated: NoInitiatedOrder ) : P[OrderEvent] = {
    
    fromCommand match {
        
      //~ case initOrder : InitOrder => (noInitiated: OrderEvent).point
      case initOrder : InitOrder => E pure noInitiated
      case command   : OrderCommand => E raiseError OrderIsNoInitiated( command )
    
    }
    
  }
  
  private def validateStateInitiatedOrder( fromCommand : OrderCommand )( initiatedOrder : InitiatedOrder ) : P[OrderEvent] = {
    
    fromCommand match {
      
      case initOrder  : CancelOrder => (initiatedOrder: OrderEvent).point
      case closeOrder : CloseOrder  => (initiatedOrder: OrderEvent).point
      case command : OrderCommand => E raiseError OrderIsInitiated( command )
    
    }
    
  }
  

}
