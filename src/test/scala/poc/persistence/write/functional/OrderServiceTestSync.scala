package poc.persistence.write.functional
import scalaz.\/, \/.{left, right}
import scalaz.{\/-, -\/} 

object OrderServiceTestSync {
    
    import scalaz.MonadError
  
    type ProgramSync[T] =  Error \/ T
    
    implicit val E: MonadError[ProgramSync,Error] = new MonadError[ProgramSync,Error]{
    
      def point[A](a: => A): ProgramSync[A] = \/-( a )
      
      // Members declared in scalaz.Bind
      def bind[A, B](fa: ProgramSync[A])(f: A => ProgramSync[B]): ProgramSync[B] = fa flatMap f
      
      // Members declared in scalaz.MonadError
      def handleError[A](fa: ProgramSync[A])(f: Error => ProgramSync[A]): ProgramSync[A] = fa
      
      def raiseError[A](e: Error): ProgramSync[A] = -\/( e )
      
    }
    
    implicit val orderServiceTestSync = new OrderServiceTestSync
  
}

import OrderServiceTestSync._

class OrderServiceTestSync extends OrderService[ProgramSync]  {
  
  import scalaz.MonadError  
  
  import scalaz.syntax.monadError._
  import scalaz.syntax._
  
  implicit val E: MonadError[ProgramSync,Error] = OrderServiceTestSync.E
  
  def getLastEvent(idOrder: String): ProgramSync[OrderEvent] =  E point NoInitiatedOrder( idOrder, 1, 0 ) 
  
  def saveCommand(command: OrderCommand)(version: Long): ProgramSync[ OrderEvent ] = {
    
      if ( version != 0 ) {
        
        E raiseError VersionOutOfDate( command.idOrder, version )
        
      } else {
          
         E point InitiatedOrder( command.idOrder, 1, 1 )
        
      }
    
  }  
  
}

