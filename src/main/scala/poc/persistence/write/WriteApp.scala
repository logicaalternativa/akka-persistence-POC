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

  implicit val system = ActorSystem("example")
  
  import scala.concurrent.duration._
  import scala.concurrent.Future
  implicit val timeout = akka.util.Timeout(10 seconds)
  
  import system.dispatcher 
  import akka.pattern.ask
  
  import poc.persistence.UtilActorSystem._
  import poc.persistence.HelperConsole._
  import java.lang.System._
  
  
  def menu( implicit orderReceiver: ActorRef, system : ActorSystem ) : Unit = {
    
    println( "\nChoose Command?  " )
    println( "[ 1 ] : Create fast InitializeOrder (order1, user1)" )
    println( "[ 2 ] : Create fast CancelOrder (orde1,user1) " )
    println( "[ 3 ] : Create InitializeOrder" )
    println( "[ 4 ] : Create CancelOrder" )
    println( "[ 0 ] : Exit " )
     
    val id = readLongFromConsole
    
    if ( id == 1 ) {
      
      sendMessage( 1, 1, ConsoleHelp.INIT )
     
    } else if ( id == 2) {
     
      sendMessage( 1, 1, ConsoleHelp.CANCEL )
     
    } else if ( id == 3 ) {
      
      menuAddIdOrder( ConsoleHelp.INIT )
      
    } else if ( id == 4 ) {
      
      menuAddIdOrder( ConsoleHelp.CANCEL )
            
    } else if ( id <= 0 ) {
		
      proccessTerminate( terminate )
      
    } else{ 
      
      menu
      
    }
  }
  
  def menuAddIdOrder( tpMsg : TypeMsgConsole )( implicit orderReceiver: ActorRef, system : ActorSystem ) {
    
    println( "\nId order?  " )
    println( "[ 0 ] : Exit " )
    
    val idOrder = readLongFromConsole
    
    if ( idOrder <= 0 ) {
      
      menu
        
    } else {
      
      menuAddIdUser( idOrder, tpMsg )
      
    }
    
  }
  
  def menuAddIdUser( idOrder : Long, typeMsg : TypeMsgConsole )(implicit orderReceiver: ActorRef, system : ActorSystem ) = {
    
    println( s"\nId user from order '$idOrder'?  " )
    println( "[ 0 ] : Exit " )
    
    val idUser = readLongFromConsole
    
    if ( idUser <= 0 ) {
      
      menu
        
    } else {
      
      sendMessage( idOrder, idUser, typeMsg )
      
    }    
  }
  
  def sendMessage( idOrder : Long, idUser: Long, typeMsg : TypeMsgConsole )( implicit orderReceiver: ActorRef, system : ActorSystem )  = {
    
    typeMsg match {        
          
          case ConsoleHelp.INIT =>
          
            val msg = InitializeOrder( currentTimeMillis, s"order$idOrder", idUser)
            
            proccessResponseReturnMenu( orderReceiver ? msg )
            
          case ConsoleHelp.CANCEL =>
          
            val msg = CancelOrder( currentTimeMillis, s"order$idOrder", idUser)
            
            proccessResponseReturnMenu( orderReceiver ? msg )
      }
    
  }
  
  def proccessResponseReturnMenu( future : Future[Any] ) (implicit orderReceiver: ActorRef, system : ActorSystem ) {
  
    processResponse( future ) {
          () => menu
    }
    
  }
  
  starShardingRegions

  implicit val orderReceiver: ActorRef = OrderActor.receiver

  menu

}
