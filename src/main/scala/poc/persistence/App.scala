package poc.persistence

import akka.actor._
import akka.pattern.ask

import scala.util.{Success, Failure}

sealed trait TypeMsgConsole

object ConsoleHelp {

  case object INIT extends TypeMsgConsole

  case object CLOSE extends TypeMsgConsole
  
  case object CANCEL extends TypeMsgConsole

}


object AppCQRS extends App {
  
  import poc.persistence.functional.Poc.implicits._
  import poc.persistence.functional.Poc.ProgramAsync
  import poc.persistence.HelperConsole._
  
  
  def menu : Unit = {
    
    import poc.persistence.UtilActorSystem.terminate
    
    
    println( "\nChoose Command?  " )
    println( "[ 1 ] : Create fast InitOrder (order1, user1)" )
    println( "[ 2 ] : Create fast CloseOrder(orde1,user1 ) " )
    println( "[ 3 ] : Create fast CancelOrder (orde1,user1) " )
    println( "[ 4 ] : Create InitializeOrder" )
    println( "[ 5 ] : Create CloseOrder" )
    println( "[ 6 ] : Create CancelOrder" )
    println( "[ 7 ] : Get user history" )
    println( "[ 0 ] : Exit " )
     
    val id = readLongFromConsole
    
    if ( id == 1 ) {
      
      sendMessage( 1, 1, ConsoleHelp.INIT )
     
    } else if ( id == 2) {
     
      sendMessage( 1, 1, ConsoleHelp.CLOSE )
     
    } else if ( id == 3) {
     
      sendMessage( 1, 1, ConsoleHelp.CANCEL )
     
    } else if ( id == 4 ) {
      
      menuAddIdOrder( ConsoleHelp.INIT )
      
    } else if ( id == 5 ) {
      
      menuAddIdOrder( ConsoleHelp.CLOSE )
            
    } else if ( id == 6 ) {
      
      menuAddIdOrder( ConsoleHelp.CANCEL )
            
    } else if ( id == 7 ) {
      
      menuGetHistory
            
    } else if ( id <= 0 ) {
		
      proccessTerminate( terminate )
      
    } else { 
      
      menu
      
    }
  }
  
  def menuAddIdOrder( tpMsg : TypeMsgConsole ) {
    
    println( "\nId order?  " )
    println( "[ 0 ] : Exit " )
    
    val idOrder = readLongFromConsole
    
    if ( idOrder <= 0 ) {
      
      menu
        
    } else {
      
      menuAddIdUser( idOrder, tpMsg )
      
    }
    
  }
  
  def menuGetHistory : Unit = {
    
    import poc.persistence.functional.read.UserViewService.Syntax._
    
    println( s"\nId User?  " )
    println( "[ 0 ] : Exit from get history " )
    
    
    val idUser = readLongFromConsole
    
    if ( idUser <= 0 ) {
      
      menu
        
    } else {
      
      val res = getHistory[ProgramAsync]( idUser )
            
      proccessResponseReturnNext( res, menuGetHistory _  )
      
    }    
  }
  
  
  
  def menuAddIdUser( idOrder : Long, typeMsg : TypeMsgConsole ) = {
    
    println( s"\nId user from order '$idOrder'?  " )
    println( "[ 0 ] : Exit " )
    
    val idUser = readLongFromConsole
    
    if ( idUser <= 0 ) {
      
      menu
        
    } else {
      
      sendMessage( idOrder, idUser, typeMsg )
      
    }    
  }
  
  def sendMessage( idOrder : Long, idUser: Long, typeMsg : TypeMsgConsole )  = {
    
    import poc.persistence.functional.Poc.implicits._
    import poc.persistence.functional.ApplicationFlow
    import poc.persistence.functional.write.{InitOrder,CloseOrder,CancelOrder,OrderCommand}
    
    val msg = typeMsg match {        
          
          case ConsoleHelp.INIT => InitOrder(  s"order$idOrder", idUser)
                        
          case ConsoleHelp.CLOSE => CloseOrder( s"order$idOrder", idUser)
          
          case ConsoleHelp.CANCEL => CancelOrder( s"order$idOrder", idUser)
      }
      
    val res = ApplicationFlow.write[ProgramAsync]( msg )
            
    proccessResponseReturnMenu( res  )

    
  }
  
  def proccessResponseReturnMenu[P]( future : ProgramAsync[P] ) {
    
    proccessResponseReturnNext( future, menu _  )
    
  }
  
  def proccessResponseReturnNext[P]( future : ProgramAsync[P], next : () => Unit ) {
    
    future onComplete( _ match {
        
        case Success( s )  =>  {
            
            val log = s.fold( 
              error => s"OUT (with ERROR) => $error", 
              value => s"OUT OK => $value"
            )
            
            println( s"\n********* Result **********************" )
            println( s"$log" )
            println( s"********* End of result ***************\n" ) 
            next()
          }
          
        case  Failure( e ) =>  {
            println( s"\n********* Error ****************" )
            println( s"${e.getMessage}" )
            println( s"********* End of error *********\n" )
            next()
          }
        
      }
    )
    
  }
  
  menu

}
