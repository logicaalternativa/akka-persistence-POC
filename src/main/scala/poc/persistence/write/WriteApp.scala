package poc.persistence.write

import akka.actor._
import akka.pattern.ask

import scala.util.{Success, Failure}

object WriteApp extends App {
  
  import Commands._

  val system = ActorSystem("example")
  
  import scala.concurrent.duration._
  import scala.concurrent.Future
  implicit val timeout = akka.util.Timeout(10 seconds)
  
  import system.dispatcher 
  import akka.pattern.ask
  
  import poc.persistence.UtilActorSystem._
  
  
  def console( orderReceiver: ActorRef, system : ActorSystem ) : Unit = {
    
    import scala.io.StdIn._
    
    println( "Chose Command?  " )
    println( "[ 1 ] to create InitializeOrder " )
    println( "[ 2 ] to create CancelOrder " )
    println( "[ <= 0 ] to exit " )
    val id = readLong()
    
    if ( id == 1 ) {
      processResponse( orderReceiver ? InitializeOrder(1, "order1", 1) )( orderReceiver, system )
    } else if ( id == 2 ) {
      processResponse( orderReceiver ? CancelOrder(2, "order1", 1 ) )( orderReceiver, system )
    } else if ( id <= 0 ) {
      proccessTerminate( terminate( system ) )
    } else{ 
      console( orderReceiver, system )
    }
    
  }
  
  def proccessTerminate( response : Future[Terminated] ) : Unit = {
    
    response.onComplete {
      case Success(msg) =>
        println(s"········· Actor system terminated succesfully -> $msg ·········")
      case Failure(e) =>
        println(s"········· Error stopping actor system -> ${e.getMessage} ·········")
      }
    
  }
  
  def processResponse( response : Future[Any] )( orderReceiver:ActorRef, system : ActorSystem ) : Unit = {
    
    response.onComplete {
      case Success(msg) =>
        println( s"********* Succesfully -> $msg *********" )
        console( orderReceiver, system )
      case Failure(e) =>
        println( s"********* Error ->  ${e.getMessage} *********" )
        console( orderReceiver, system )
    }
    
  }
  
  starShardingRegions( system )

  val orderReceiver: ActorRef = OrderActor.receiver( system )

  console( orderReceiver, system )
   
  

}
