package poc.persistence.functional
package publish

import scalaz.\/, \/.{left, right}
import scalaz.{\/-, -\/}


import Poc.ProgramSync
import write._

object PublishServiceTestSync {
    
    import scalaz.MonadError
  
    implicit val E : MonadError[ProgramSync,Error] = new MonadError[ProgramSync,Error]{
    
      def point[A](a: => A): ProgramSync[A] = \/-( a )
      
      // Members declared in scalaz.Bind
      def bind[A, B](fa: ProgramSync[A])(f: A => ProgramSync[B]): ProgramSync[B] = fa flatMap f
      
      // Members declared in scalaz.MonadError
      def handleError[A](fa: ProgramSync[A])(f: Error => ProgramSync[A]): ProgramSync[A] = fa
      
      def raiseError[A](e: Error): ProgramSync[A] = -\/( e )
      
    }
    
    implicit val PublishServiceTestSync = new PublishServiceTestSync
  
}

import PublishServiceTestSync._

class PublishServiceTestSync extends PublishService[ProgramSync]  {
  
  import scalaz.MonadError
  import scalaz.syntax._
  import scalaz.syntax.monadError._
  
  implicit val E: MonadError[ProgramSync,Error] = PublishServiceTestSync.E
  
  def publish( orderEvent:OrderEvent ) : ProgramSync[Long] = E point 1L
  
  def sendAck(idMessage: Long, idOrder : String, version : Long) : ProgramSync[Boolean] = (false).pure
    
  
}

