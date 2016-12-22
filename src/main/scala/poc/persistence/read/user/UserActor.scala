package poc.persistence.read.user

import akka.actor.{ActorLogging, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted}
import events._
import poc.persistence.events.{OrderCancelled, OrderEvent, OrderInitialized}
import queries._

object UserActor {

  def name = "Users"

  def props = Props[UserActor]

  // the input for the extractShardId function
  // is the message that the "handler" receives
  def extractShardId: ShardRegion.ExtractShardId = {
    case msg: OrderInitialized =>
      (msg.idUser % 2).toString
    case msg: OrderCancelled =>
      (msg.idUser % 2).toString
    case msg: GetHistoryFor =>
      (msg.idUser % 2).toString
  }

  // the input for th extractEntityId function
  // is the message that the "handler" receives
  def extractEntityId: ShardRegion.ExtractEntityId = {
    case event: OrderInitialized =>
      (event.idUser.toString, event)
    case event: OrderCancelled =>
      (event.idUser.toString, event)
    case query: GetHistoryFor =>
      (query.idUser.toString, GetHistory)
  }

}

class UserActor extends PersistentActor with ActorLogging {

  var myEvents = List[UserEvent]()

  override def persistenceId: String = self.path.name

  override def receiveCommand = {
    // nothing interesting, just "translation" of events
    case OrderCancelled(idOrder, idUser) =>
      persist(UserCancelledOrder(idOrder, idUser)) {
        e => {
         onEvent(e)
      }}
    case OrderInitialized(idOrder, idUser) =>
      persist(UserInitializedOrder(idOrder, idUser)) {
        e => {
          onEvent(e)
        }
      }
    case GetHistory =>
      sender ! History ( myEvents.reverse.map {
        case event: UserInitializedOrder => "UserInitializedOrder" -> event
        case event: UserCancelledOrder => "UserCancelledOrder" -> event
      } )

  }

  def onEvent(e: UserEvent) = {
    myEvents = e :: myEvents
  }

  override def receiveRecover: Receive = {
    case e: UserEvent =>
      onEvent(e)
  }

}