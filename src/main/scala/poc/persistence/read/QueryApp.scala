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
  import poc.persistence.HelperConsole._
    
  def console( handlerForUsers: ActorRef, system : ActorSystem ) : Unit = {
    
    import scala.io.StdIn._
    
    println( "\nHistory for id user? " )
    println( "[0] : Exit " )
    val userId = readLongFromConsole
    
    if ( userId <= 0  ) {
      
      proccessTerminate( terminate( system ) )
      
    } else {
      
      processResponse( handlerForUsers ? GetHistoryFor( userId ) ) {
        () => console( handlerForUsers, system )
      }
      
    }
  }
  
  starShardingRegions( system )
  
  val handlerForUsers: ActorRef =  UserActor.receiver( system )
  
  console( handlerForUsers, system )
  
}


