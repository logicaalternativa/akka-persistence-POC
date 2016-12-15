package poc.persistence.read

import akka.actor.{ActorSystem, _}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.json4s.{DefaultFormats, jackson}
import poc.persistence.events.{OrderCancelled, OrderInitialized}
import poc.persistence.read.streammanager.StreamManager
import poc.persistence.read.streammanager.commands.SaveProgress
import poc.persistence.read.streammanager.queries.GetLastOffsetProcessed
import poc.persistence.read.user.UserActor
import poc.persistence.read.user.queries.{GetHistoryFor, History}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object ReadApp extends App {

  implicit val timeout = akka.util.Timeout(10 seconds)

  implicit val system = ActorSystem("example")
  implicit val materializer = ActorMaterializer()

  val logger = Logging.getLogger(system, this)

  import system.dispatcher

  ClusterSharding(system).start(
    typeName = UserActor.name,
    entityProps = UserActor.props,
    settings = ClusterShardingSettings(system),
    extractShardId = UserActor.extractShardId,
    extractEntityId = UserActor.extractEntityId
  )

  val handlerForUsers: ActorRef = ClusterSharding(system).shardRegion(UserActor.name)

  val streamManager = system.actorOf(StreamManager.props, "stream-manager")

  val askForLastOffset: Future[Long] = (streamManager ? GetLastOffsetProcessed).mapTo[Long]

  askForLastOffset.onSuccess {
    case lastOffset: Long =>
      PersistenceQuery(system)
        .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
        .eventsByTag("UserEvent", lastOffset + 1)
        .map { envelope => {
          envelope.event match {
            case e: OrderInitialized =>
              handlerForUsers ! e
              streamManager ! SaveProgress(envelope.offset)
            case e: OrderCancelled =>
              handlerForUsers ! e
              streamManager ! SaveProgress(envelope.offset)
          }
        }
        }.runWith(Sink.ignore)
  }

  askForLastOffset.onFailure {
    case NonFatal(e)=>
      logger.error(e, "failed to get last offset")
  }


  import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats

  val route = get {
    path("users" / Segment) {
      userId: String => {
        onComplete(handlerForUsers ? GetHistoryFor(userId.toLong)) {
          case Success(History(namedEvents)) => complete(namedEvents)
          case Failure(_) | Success(_) => complete(InternalServerError -> Map("mesage" -> "internal server error"))
        }
      }
    }
  }

  Http().bindAndHandle(route, "localhost", 8080)

}

