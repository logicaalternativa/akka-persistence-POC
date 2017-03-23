package poc.persistence.functional


import poc.persistence.functional.write.{OrderCommand, OrderEvent}

// Errors
sealed abstract class Error extends Throwable

case class ErrorActorOrder( idOrder : String, e : Throwable ) extends Error
case class ErrorActorUser( idUser : Long, e : Throwable ) extends Error

case class OrderIsClosed( idOrder : String ) extends Error
case class OrderIsCanceled( idOrder : String ) extends Error
case class OrderIsNoInitiated( command : OrderCommand ) extends Error
case class OrderIsInitiated( command : OrderCommand ) extends Error
case class OrderNotYetInitialized( idOrder : String ) extends Error
case class OrderVersionNoOk( event: OrderEvent, lastVersion : Long) extends Error

case class VersionOutOfDate( idOrder : String, version : Long ) extends Error


object Poc {
  
  
  import scalaz.{\/-, -\/} 
  import scalaz.\/, \/.{left, right}
  
  import scalaz.{Monad, Reader,MonadError}
  
  import scalaz.syntax._
  import scalaz.syntax.monadError._
  
  import scala.concurrent.{Future, ExecutionContext, Await}
  import scala.util.Try
  import scala.concurrent.duration._
  
  import akka.pattern.ask
  import akka.actor.{ActorSystem, ActorRef}
  import akka.cluster._
  
  import poc.persistence.functional.publish.{PublishServiceActor,PublishService}
  import poc.persistence.functional.read.UserViewServiceActor
  import poc.persistence.functional.write.OrderServiceActor
  import poc.persistence.publish.PublishActor
  import poc.persistence.read.UserActorFunctional
  import poc.persistence.stream.StreamToQueryFunctional
  import poc.persistence.write.OrderActorFunctional
  
  type ProgramAsync[T] = Future[Error \/ T]
  type ProgramSync[T] = Error \/ T
  
  /**
   * */
  def createMonadError( implicit ec: ExecutionContext )  = {
    
    new MonadError[ProgramAsync,Error]{
      
      // Members declared in scalaz.Applicative
      def point[A](a: => A): ProgramAsync[A] = Future( \/-( a ) )
      
      // Members declared in scalaz.Bind
      def bind[A, B](fa: ProgramAsync[A])(f: A => ProgramAsync[B]): ProgramAsync[B] = {
        fa flatMap ( _.fold( error => raiseError( error ), value => f(value) ) )
        
      }
      
      // Members declared in scalaz.MonadError
      def handleError[A](fa: ProgramAsync[A])(f: Error => ProgramAsync[A]): ProgramAsync[A] = fa
      
      def raiseError[A](e: Error): ProgramAsync[A] = Future( -\/( e ) )
      
    }
    
    
  }
  
  /**
   * */  
  def createMonadAsync( implicit ec: ExecutionContext ) = {
    
      new Monad[ ProgramAsync ] {
      
        def point[A](a: => A)  : ProgramAsync[A] =  Future( right( a ) )
        
        def bind[A, B](fa: ProgramAsync[A])(f: A => ProgramAsync[B]): ProgramAsync[B] = {
          
          fa flatMap ( _.fold( error => Future( left (error) ), value => f( value ) ) )
          
        }
        
      }
  }
  
  
  implicit val system = ActorSystem("example")
  
  Cluster(system).registerOnMemberRemoved {
  // Traza
  
  println( "I run away!!!" )
  
  // Fin de traza
  
  
  // exit JVM when ActorSystem has been terminated
  system.registerOnTermination(System.exit(0))
  // shut down ActorSystem
  system.terminate()

  // In case ActorSystem shutdown takes longer than 10 seconds,
  // exit the JVM forcefully anyway.
  // We must spawn a separate thread to not block current thread,
  // since that would have blocked the shutdown of the ActorSystem.
  new Thread {
    override def run(): Unit = {
      if (Try(Await.ready(system.whenTerminated, 10.seconds)).isFailure)
        System.exit(-1)
    }
  }.start()
}
  
  implicit val executionContex = system.dispatcher

  val orderActorFunct   : ActorRef = OrderActorFunctional.startRegion
  val publishActorFunct : ActorRef = PublishActor.startRegion
  val userActorFunct    : ActorRef = UserActorFunctional.startRegion

  implicit val orderSrvActor    = OrderServiceActor.apply
  implicit val publishSrvActor : PublishService[ProgramAsync]  = PublishServiceActor.apply
  implicit val userViewSrvActor = UserViewServiceActor.apply

  implicit val monadError = createMonadError
  implicit val monadAsync = createMonadAsync

  val streamActor : ActorRef =  StreamToQueryFunctional.actorStream[ProgramAsync]
  
  object implicits {
      implicit val a = system
      implicit val b = executionContex
      implicit val c = orderSrvActor
      implicit val d = publishSrvActor
      implicit val e = userViewSrvActor
      implicit val f = monadError
      implicit val g = monadAsync
  }
  
}

