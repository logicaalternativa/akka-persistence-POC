package poc.persistence.write.functional
import scalaz.\/, \/.{left, right}
import scalaz.{\/-, -\/} 
import scala.concurrent.{Future, ExecutionContext}

object OrderServiceTestAsync {
  
    import scala.concurrent.{Future, ExecutionContext}
    
    import scalaz.{Monad, MonadError}
    import scalaz.syntax._
    
    implicit val ec = ExecutionContext.global
  
    //~ type ProgramAsync[T] =  Future[Error \/ T]
    type ProgramAsync[T] =  Future[T]
    
    implicit val E: MonadError[ProgramAsync,Error] = new MonadError[ProgramAsync,Error]{
      
      // Members declared in scalaz.Applicative
      //~ def point[A](a: => A): ProgramAsync[A] = Future( \/-( a ) )
      def point[A](a: => A): ProgramAsync[A] = Future( a )
      
      // Members declared in scalaz.Bind
      def bind[A, B](fa: ProgramAsync[A])(f: A => ProgramAsync[B]): ProgramAsync[B] = fa flatMap f
      
      // Members declared in scalaz.MonadError
      def handleError[A](fa: ProgramAsync[A])(f: Error => ProgramAsync[A]): ProgramAsync[A] = fa
      
      def raiseError[A](e: Error): ProgramAsync[A] =Future.failed( e )
      
    }
    
    implicit val futureMonad = new Monad[Future] {
      
      override def point[A](a: ⇒ A): Future[A] = Future(a)
      
      override def bind[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa flatMap f
      
    }
    
    implicit val orderServiceTestAsync : OrderServiceTestAsync = new OrderServiceTestAsync
   
  
}

import OrderServiceTestAsync._


class OrderServiceTestAsync extends OrderService[Future]  {
  
  import scalaz.MonadError  
  
  import scalaz.syntax.monadError._
  import scalaz.syntax._
  
  implicit val E: MonadError[ProgramAsync,Error] = OrderServiceTestAsync.E
  
  def getLastEvent(idOrder: String): ProgramAsync[OrderEvent] =  E point NoInitiatedOrder( idOrder, 1, 0 ) 
  
  def saveCommand(command: OrderCommand)(version: Long): ProgramAsync[ OrderEvent ] = {
    
      if ( version != 0 ) {
        
        E raiseError VersionOutOfDate( command.idOrder, version )
        
      } else {
          
         E point InitiatedOrder( command.idOrder, 1, 1 )
        
      }
    
  }  
  
}

