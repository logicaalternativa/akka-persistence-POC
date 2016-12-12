package poc.persistence.write

import akka.actor._
import akka.pattern.ask

import scala.util.{Success, Failure}

sealed trait TypeMsgConsole

object ConsoleHelp {

  case object INIT extends TypeMsgConsole

  case object CANCEL extends TypeMsgConsole

}


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
  import java.lang.System._
  
  
  def menu( orderReceiver: ActorRef, system : ActorSystem ) : Unit = {
    
    println( "\nChoose Command?  " )
    println( "[ 1 ] : Create fast InitializeOrder (order1, user1)" )
    println( "[ 2 ] : Create fast CancelOrder (orde1,user1) " )
    println( "[ 3 ] : Create InitializeOrder" )
    println( "[ 4 ] : Create CancelOrder" )
    println( "[ 0 ] : Exit " )
     
    val id = readLongFromConsole
    
    if ( id == 1 ) {
      
      sendMessage( 1, 1, ConsoleHelp.INIT )( orderReceiver, system )
     
    } else if ( id == 2) {
     
      sendMessage( 1, 1, ConsoleHelp.CANCEL )( orderReceiver, system )
     
    } else if ( id == 3 ) {
      
      menuAddIdOrder( ConsoleHelp.INIT )( orderReceiver, system )
      
    } else if ( id == 4 ) {
      
      menuAddIdOrder( ConsoleHelp.CANCEL )( orderReceiver, system )
      
    } else if ( id <= 0 ) {
      proccessTerminate( terminate( system ) )
    } else{ 
      menu( orderReceiver, system )
    }
  }
  
  def menuAddIdOrder( tpMsg : TypeMsgConsole )( orderReceiver: ActorRef, system : ActorSystem ) {
    
    println( "\nId order?  " )
    println( "[ 0 ] : Exit " )
    
    val idOrder = readLongFromConsole
    
    if ( idOrder <= 0 ) {
      
      menu( orderReceiver, system )
        
    } else {
      
      menuAddIdUser( idOrder, tpMsg ) ( orderReceiver, system )
      
    }
    
  }
  
  def menuAddIdUser( idOrder : Long, typeMsg : TypeMsgConsole )(orderReceiver: ActorRef, system : ActorSystem ) = {
    
    println( "\nId user?  " )
    println( "[ 0 ] : Exit " )
    
    val idUser = readLongFromConsole
    
    if ( idUser <= 0 ) {
      
      menu( orderReceiver, system )
        
    } else {
      
      sendMessage( idOrder, idUser, typeMsg )( orderReceiver, system )
      
    }    
  }
  
  def sendMessage( idOrder : Long, idUser: Long, typeMsg : TypeMsgConsole )(orderReceiver: ActorRef, system : ActorSystem )  = {
    
    typeMsg match {        
          
          case ConsoleHelp.INIT =>
          
            val msg = InitializeOrder( currentTimeMillis, s"order$idOrder", idUser)
            
            proccessResponseReturnMenu( orderReceiver ? msg )( orderReceiver, system )
            
          case ConsoleHelp.CANCEL =>
          
            val msg = CancelOrder( currentTimeMillis, s"order$idOrder", idUser)
            
            proccessResponseReturnMenu( orderReceiver ? msg )( orderReceiver, system )
            
      }
    
  }
  
  def proccessResponseReturnMenu( future : Future[Any] ) (orderReceiver: ActorRef, system : ActorSystem ) {
  
    processResponse( future ) {
          () => menu( orderReceiver, system )
    }
    
  }
  
  starShardingRegions( system )

  val orderReceiver: ActorRef = OrderActor.receiver( system )

  menu( orderReceiver, system )

}
