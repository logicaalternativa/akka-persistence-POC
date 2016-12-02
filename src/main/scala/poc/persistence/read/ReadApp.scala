package poc.persistence.read

import akka.actor.Status.Success
import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import akka.pattern.ask
import poc.persistence.write.OrderActor

import scala.concurrent.Future
import akka.actor._

object ReadApp extends App {

  import akka.persistence.query._

  implicit val system = ActorSystem("example")

  ClusterSharding(system).start(
    typeName = UserActor.name, // orders
    entityProps = UserActor.props,
    settings = ClusterShardingSettings(system),
    extractShardId = UserActor.extractShardId,
    extractEntityId = UserActor.extractEntityId
  )

  val streamManager = system.actorOf(StreamManager.props)

  val askForLastOffset: Future[Any] = streamManager ? GetLastOffsetProc

  askForLastOffset.mapTo[Long].onSuccess {
    case lastOffset: Long =>
      val query = PersistenceQuery(system)
        .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
        .eventsByTag("HasUserId", Offset.sequence(lastOffset))
        .map { envelope => {
          envelope.event match {
            case e: poc.persistence.write.Events.OrderInitialized => {
              val userId = e.order.idUser
              getUserChildActor(userId) ! e
              streamManager ! SaveProgress(envelope.sequenceNr)
            }
            case e: poc.persistence.write.Events.OrderCancelled => {
              val userId = e.order.idUser
              getUserChildActor(userId) ! e
              streamManager ! SaveProgress(envelope.sequenceNr)
            }
          }
        }
        }


  }


}


object StreamManager {

  def props = Props[StreamManager]

}

case object GetLastOffsetProc

case class SaveProgress(i: Long)

case class ProgressAcknowledged(i: Long)

class StreamManager extends PersistentActor with ActorLogging {

  var lastOffsetProc: Long = 0L // initial value is 0

  implicit val mat = ActorMaterializer()

  override def receiveRecover: Receive = {
    case ProgressAcknowledged(i) =>
      lastOffsetProc = i
  }

  override def receiveCommand: Receive = {
    case GetLastOffsetProc =>
      sender ! lastOffsetProc
    case SaveProgress(i: Long) =>
      persist(ProgressAcknowledged(i)) {
        e => {
          lastOffsetProc = e.i
          sender ! 'Success
        }
      }
  }

  def onEvent(e: AnyRef) = ??? // implement mas tarde

  override def persistenceId: String = "stream-manager"
}


import poc.persistence.write.Events.{OrderInitialized, OrderCancelled}
import poc.persistence.write.Event

sealed trait Query

case object GetHistory extends Query

object UserActor {

  def name = "Users"

  def props = Props[UserActor]


  // the input for the extractShardId function
  // is the message that the "handler" receives
  def extractShardId: ShardRegion.ExtractShardId = {
    case msg: OrderInitialized=>
      (msg.timeStamp % 2).toString
  }

  // the input for th extractEntityId function
  // is the message that the "handler" receives
  def extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: OrderInitialized=>
      (msg.order.idUser, msg)
  }


}

class UserActor extends Actor with ActorLogging {

  var history: List[poc.persistence.write.Event] = List()

  def receive = {
    case e: Event =>
      history = e :: history
    case GetHistory =>
      sender ! history
  }

}
