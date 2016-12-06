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

  import scala.concurrent.duration._
  implicit val timeout = akka.util.Timeout(10 seconds)
  import java.util.UUID

  implicit val system = ActorSystem("example")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  UserActor.startRegion( system )
  
  val handlerForUsers: ActorRef = UserActor.handlerForUsers( system )

  val streamManager = system.actorOf(StreamManager.props)

  val askForLastOffset: Future[Any] = streamManager ? GetLastOffsetProc

  askForLastOffset.onFailure {
    case _ => {
      println("^^^^^^^^^ Failed to get last offset ^^^^^^^^^")
    }
  }

  askForLastOffset.mapTo[Offset].onSuccess {
    case lastOffset: Offset =>
      println(s"^^^^^^^^^ We know the last offset -> $lastOffset ^^^^^^^^^")
      val query = PersistenceQuery(system)
        .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
        .eventsByTag("42", lastOffset )
        //~ .eventsByTag("42", NoOffset )
        .map { envelope => {
            envelope.event match {
              case e: poc.persistence.write.Events.OrderInitialized => {
                handlerForUsers ! e
                streamManager ! SaveProgress(envelope.offset)
                println(s"^^^^^^^^^ OrderInitialized Saved Progress ->  ${envelope.offset} ^^^^^^^^^")
                envelope
              }
              case e: poc.persistence.write.Events.OrderCancelled => {
                handlerForUsers ! e
                streamManager ! SaveProgress(envelope.offset)
                println(s"^^^^^^^^^ OrderCancelled Saved Progress ->  ${envelope.offset} ^^^^^^^^^")
                envelope
              }
              case _ =>
                println("^^^^^^^^^ I don't understand ^^^^^^^^^")
                envelope
            }
          }
        }
        .runForeach(f => println(s"^^^^^^^^^ Processed one element! -> $f ^^^^^^^^^"))
  }
}


