package poc.persistence

import akka.actor._
import akka.pattern.ask

import scala.util.{Success, Failure}
import scala.concurrent.{Future, ExecutionContext }

object HelperConsole extends App {
  
  def readLongFromConsole : Long = {
        
    import scala.io.StdIn._
    
    try {
      readLong
    } catch {
        case e: Exception => 
          println( s"Error to read long from console -> ${e.getMessage}"  )
          println( "Please, try again"  )
          readLongFromConsole
    }
    
  }
  
  def proccessTerminate( response : Future[Terminated])( implicit ec : ExecutionContext ) : Unit = {
    
    println ( s"Terminating...")
    
    response.onComplete {
    
      case Success( msg ) =>
        println(s"········· Actor system terminated succesfully -> $msg ·········")
      case Failure(e) =>
        println(s"········· Error stopping actor system -> ${e.getMessage} ·········")
    
    }( ec )
    
  }
  
  def processResponse( response : Future[Any])( next: () => Unit )( implicit ec : ExecutionContext ) : Unit = {
    
    response.onComplete {
     
      case Success(msg) =>
        println( s"\n********* Result **********************" )
        println( s"$msg" )
        println( s"********* End of result ***************\n" )
        next()
      case Failure(e) =>
        println( s"\n********* Error ****************" )
        println( s"${e.getMessage}" )
        println( s"********* End of error *********\n" )
        next()
        
    }( ec )
    
  }

}
