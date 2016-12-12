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
  import poc.persistence.HelperConsole._
  
  def console( orderReceiver: ActorRef, system : ActorSystem ) : Unit = {
    
    println( "\nChose Command?  " )
    println( "[ 1 ] : Create InitializeOrder " )
    println( "[ 2 ] : Create CancelOrder " )
    println( "[ 0 ] : Exit " )
     
    val id = readLongFromConsole
    
    if ( id == 1 ) {
      processResponse( orderReceiver ? InitializeOrder(1, "order1", 1) ) {
        () => console( orderReceiver, system )
      }
    } else if ( id == 2 ) {
      processResponse( orderReceiver ? CancelOrder(2, "order1", 1 ) ) {
        () => console( orderReceiver, system )
      }
    } else if ( id <= 0 ) {
      proccessTerminate( terminate( system ) )
    } else{ 
      console( orderReceiver, system )
    }
    
  }
  
  
  starShardingRegions( system )

  val orderReceiver: ActorRef = OrderActor.receiver( system )

  console( orderReceiver, system )
   
  

}
