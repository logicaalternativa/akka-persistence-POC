package poc.persistence.read

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.event.Logging
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query._
import akka.stream.ActorMaterializer
import poc.persistence.read.StreamManager.{GetLastOffsetProcessed, ProgressAcknowledged, SaveProgress}
import poc.persistence.read.UserActor.GetHistory
import poc.persistence.write.Events.OrderCancelled

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

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

  val streamManager = system.actorOf(StreamManager.props)

  val askForLastOffset: Future[Any] = (streamManager ? GetLastOffsetProcessed).mapTo[Long]

  askForLastOffset.onSuccess {
    case lastOffset: Long =>
      logger.debug("last offset is equal to {}", lastOffset)
      PersistenceQuery(system)
        .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
        .eventsByTag("UserEvent", Offset.sequence(lastOffset))
        .map { envelope => {
          envelope.event match {
            case e: poc.persistence.write.Events.OrderInitialized =>
              handlerForUsers ! e
              streamManager ! SaveProgress(envelope.sequenceNr)
            case e: poc.persistence.write.Events.OrderCancelled =>
              handlerForUsers ! e
              streamManager ! SaveProgress(envelope.sequenceNr)
          }
        }
        }.runForeach(_ => ())
  }

}


object StreamManager {

  def props = Props[StreamManager]

  case object GetLastOffsetProcessed

  case class SaveProgress(i: Long)

  case class ProgressAcknowledged(i: Long)
}


class StreamManager extends PersistentActor with ActorLogging {

  override def persistenceId: String = "stream-manager"

  var lastOffsetProc: Long = 0L // initial value is 0

  override def receiveRecover: Receive = {
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


import poc.persistence.write.Event
import poc.persistence.write.Events.OrderInitialized

object UserActor {

  def name = "Users"

  def props = Props[UserActor]


  sealed trait Query

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
    case msg: OrderInitialized =>
      (msg.idUser.toString, msg)
    case msg: OrderCancelled =>
      (msg.idUser.toString, msg)
    case msg: GetHistoryFor =>
      (msg.idUser.toString, GetHistory)
  }

}

class UserActor extends Actor with ActorLogging {

  var history: List[poc.persistence.write.Event] = List()

  override def receive = {
    case e: Event =>
      log.info("received event!")
      history = e :: history
    case GetHistory =>
      sender ! history
  }

}