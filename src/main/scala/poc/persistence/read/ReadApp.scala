package poc.persistence.read

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.stream.ActorMaterializer
import poc.persistence.write.Events.OrderCancelled

import scala.concurrent.Future
import scala.language.postfixOps

/*
object ReadApp extends App {


  import scala.concurrent.duration._
  implicit val timeout = akka.util.Timeout(10 seconds)

  import akka.persistence.query._

  implicit val system = ActorSystem("example")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  ClusterSharding(system).start(
    typeName = UserActor.name,
    entityProps = UserActor.props,
    settings = ClusterShardingSettings(system),
    extractShardId = UserActor.extractShardId,
    extractEntityId = UserActor.extractEntityId
  )

  val handlerForUsers: ActorRef = ClusterSharding(system)
    .shardRegion(UserActor.name)

  val streamManager = system.actorOf(StreamManager.props)

  val askForLastOffset: Future[Any] = streamManager ? GetLastOffsetProc

  askForLastOffset.onFailure {
    case _ => {
      println("^^^^^^^^^ Failed to get last offset ^^^^^^^^^")
    }
  }

  askForLastOffset.mapTo[Long].onSuccess {
    case lastOffset: Long =>
      println("^^^^^^^^^ We know the last offset ^^^^^^^^^" + lastOffset)
      val query = PersistenceQuery(system)
        .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
        .eventsByTag("42", Offset.sequence(lastOffset))
        .map { envelope => {
          envelope.event match {
            case e: poc.persistence.write.Events.OrderInitialized => {
              handlerForUsers ! e
              streamManager ! SaveProgress(envelope.sequenceNr)
              println("^^^^^^^^^ Saved Progress ^^^^^^^^^")
            }
            case e: poc.persistence.write.Events.OrderCancelled => {
              handlerForUsers ! e
              streamManager ! SaveProgress(envelope.sequenceNr)
            }
            case _ =>
              println("^^^^^^^^^ I don't understand ^^^^^^^^^")
          }
        }
        }.runForeach(f => println("Processed one element!"))
  }



  (handlerForUsers ? GetHistoryFor(1)).onSuccess {
    case s => println(s)
  }
}

object StreamManager {

  def props = Props[StreamManager]

}

case object GetLastOffsetProc

case class SaveProgress(i: Long)

case class ProgressAcknowledged(i: Long)

class StreamManager extends PersistentActor with ActorLogging {

  implicit val mat = ActorMaterializer()
  var lastOffsetProc: Long = 0L // initial value is 0

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


import poc.persistence.write.Event
import poc.persistence.write.Events.OrderInitialized

sealed trait Query

case object GetHistory extends Query

case class GetHistoryFor(idUser:Long) extends Query

object UserActor {

  def name = "Users"

  def props = Props[UserActor]


  // the input for the extractShardId function
  // is the message that the "handler" receives
  def extractShardId: ShardRegion.ExtractShardId = {
    case msg: Event =>
      (msg.timeStamp % 2).toString
    case _ => "1"
  }

  // the input for th extractEntityId function
  // is the message that the "handler" receives
  def extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: OrderInitialized =>
      (msg.order.idUser.toString, msg)
    case msg: OrderCancelled =>
      (msg.order.idUser.toString, msg)
    case msg: GetHistoryFor =>
      (msg.idUser.toString, GetHistory)

  }

}

class UserActor extends Actor with ActorLogging {

  var history: List[poc.persistence.write.Event] = List()

  override def receive = {
    case e: Event =>
      history = e :: history
    case GetHistory =>
      sender ! history.mkString(",")
  }

}

*/

