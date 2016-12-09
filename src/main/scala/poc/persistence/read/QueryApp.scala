package poc.persistence.read

import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.cluster._
import akka.pattern.ask
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.stream.ActorMaterializer

import akka.persistence.query._

import scala.concurrent.Future
import scala.language.postfixOps

import scala.util.{Success, Failure}

object QueryApp extends App {

  import scala.concurrent.duration._
  implicit val timeout = akka.util.Timeout(20 seconds)

  implicit val system = ActorSystem("example")
  import system.dispatcher
  
  import poc.persistence.UtilActorSystem._
    
  def console( handlerForUsers: ActorRef, system : ActorSystem ) : Unit = {
    
    import scala.io.StdIn._
    
    print( "\nHistory for user? (less than zero exit) " )
    val userId = readLong()
    
    if ( userId < 0  ) {
		
      terminate( system ).onComplete {
      case Success(msg) =>
        println(s"········· Actor system terminated succesfully -> $msg ·········")
      case Failure(e) =>
        println(s"········· Error stopping actor system ${e.getMessage} ·········")
      }
      
    } else {
      
      (handlerForUsers ? GetHistoryFor(userId)).onComplete {
      
      case Success(msg) =>
        println(s"********* HISTORY from user $userId -> \n $msg *********")
        console( handlerForUsers, system ) 
      case Failure(e) =>
        println(s"********* Info from the exception when is request history :  ${e.getMessage} *********")
        console( handlerForUsers, system )
      }
      
    }
  }
  
  starShardingRegions( system )
  
  val handlerForUsers: ActorRef =  UserActor.receiver( system )
  
  console( handlerForUsers, system )
  
}


