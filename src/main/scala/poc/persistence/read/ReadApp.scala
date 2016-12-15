package poc.persistence.read

import akka.actor.{ActorSystem, _}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query._
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.stream.ActorMaterializer
import org.json4s.{DefaultFormats, jackson}
import poc.persistence.events.{Event, OrderCancelled, OrderInitialized}
import poc.persistence.read.StreamManager.{GetLastOffsetProcessed, SaveProgress}
import poc.persistence.read.UserActor.{GetHistory, History}
import poc.persistence.read.events.LabelledEvent

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
        }.runForeach(_ => ())
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
        onComplete(handlerForUsers ? UserActor.GetHistoryFor(userId.toLong)) {
          case Success(History(events)) => complete(events)
          case Failure(_) | Success(_) => complete(InternalServerError -> Map("mesage" -> "internal server error"))
        }
      }
    }
  }

  Http().bindAndHandle(route, "localhost", 8080)

}


case class ProgressAcknowledged(i: Long)

object StreamManager {

  def props = Props[StreamManager]

  case object GetLastOffsetProcessed

  case class SaveProgress(i: Long)

}


class StreamManager extends PersistentActor with ActorLogging {

  override def persistenceId: String = "stream-manager"

  override def preStart(): Unit = {
    super.preStart()
    log.debug("about to start the stream manager")
  }

  var lastOffsetProc: Long = 0L // initial value is 0

  override def receiveRecover: Receive = {
    case RecoveryCompleted =>
      log.debug("recovery completed")
    case ProgressAcknowledged(i) =>
      lastOffsetProc = i
  }

  override def receiveCommand: Receive = {
    case GetLastOffsetProcessed =>
      sender ! lastOffsetProc
    case SaveProgress(i: Long) =>
      persist(ProgressAcknowledged(i)) {
        e => {
          lastOffsetProc = e.i
          sender ! 'Success
        }
      }
  }

}

package events {

  case class LabelledEvent(name: String, event: Event)

}


object UserActor {

  def name = "Users"

  def props = Props[UserActor]

  sealed trait Query

  case class History(list: List[LabelledEvent])

  case object GetHistory extends Query

  case class GetHistoryFor(idUser: Long) extends Query

  // the input for the extractShardId function
  // is the message that the "handler" receives
  def extractShardId: ShardRegion.ExtractShardId = {
    case msg: OrderInitialized =>
      (msg.idUser % 2).toString
    case msg: OrderCancelled =>
      (msg.idUser % 2).toString
    case msg: GetHistoryFor =>
      (msg.idUser % 2).toString
    case _ => "1"
  }

  // the input for th extractEntityId function
  // is the message that the "handler" receives
  def extractEntityId: ShardRegion.ExtractEntityId = {
    case event: OrderInitialized =>
      (event.idUser.toString, LabelledEvent("OrderInitialized", event))
    case event: OrderCancelled =>
      (event.idUser.toString, LabelledEvent("OrderCancelled", event))
    case query: GetHistoryFor =>
      (query.idUser.toString, GetHistory)
  }

}


class UserActor extends PersistentActor with ActorLogging {

  var allEvents = List[LabelledEvent]()

  override def persistenceId: String = self.path.name

  override def receiveCommand = {
    case e: LabelledEvent =>
      log.info("received event")
      persist(e) { e =>
        log.info("persisted event")
        onEvent(e)
      }
    case GetHistory =>
      sender ! History(allEvents.reverse)
  }

  def onEvent(e: LabelledEvent)  = {
    allEvents  = e :: allEvents
  }

  override def receiveRecover: Receive = {
    case e: LabelledEvent =>
      onEvent(e)
    case RecoveryCompleted =>
      log.debug("recovery completed")
  }

}