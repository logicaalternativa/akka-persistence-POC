package poc.persistence.read

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.ask
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.stream.ActorMaterializer

import akka.persistence.query._

import scala.concurrent.Future
import scala.language.postfixOps

object ReadApp extends App {
  
  import akka.event.Logging

  import scala.concurrent.duration._
  implicit val timeout = akka.util.Timeout(10 seconds)
  import java.util.UUID
  import poc.persistence.UtilActorSystem._

  implicit val system = ActorSystem("example")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  
  val log = Logging( system.eventStream, "poc.persistence.read.ReadApp")
  
  def console( system:ActorSystem ) : Unit = {
  
     import poc.persistence.HelperConsole._
     
     println( "Type any number to exit" )
     val id = readLongFromConsole
      proccessTerminate( terminate( system ) )
  }
  
  starShardingRegions( system )
  
  val handlerForUsers: ActorRef = UserActor.receiver( system )

  val streamManager = system.actorOf(StreamManager.props)

  val askForLastOffset: Future[Any] = streamManager ? GetLastOffsetProc

  askForLastOffset.onFailure {
    case e: Exception => {
       log.error("Failed to get last offset -> {}", e.getMessage )
    }
  }

  askForLastOffset.mapTo[Offset].onSuccess {
    case lastOffset: Offset =>
      log.info( "We know the last offset -> {}", lastOffset)
      val query = PersistenceQuery(system)
        .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
        .eventsByTag("42", lastOffset )
        .map { envelope => {
            envelope.event match {
              case e: poc.persistence.write.Events.OrderInitialized => {
                handlerForUsers ! e
                streamManager ! SaveProgress(envelope.offset)
                log.info("OrderInitialized Saved Progress -> {}", envelope.offset )
                envelope
              }
              case e: poc.persistence.write.Events.OrderCancelled => {
                handlerForUsers ! e
                streamManager ! SaveProgress(envelope.offset)
                log.info("OrderCancelled Saved Progress -> {}", envelope.offset )
                envelope
              }
              case msg =>
                log.info("I don't understand -> {}", msg)
                envelope
            }
          }
        }
        .runForeach( log.info("Processed one element! -> {}", _ ) )
        
  }
  
  console( system )
  
}


