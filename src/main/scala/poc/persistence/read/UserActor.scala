package poc.persistence.read

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.stream.ActorMaterializer
import poc.persistence.write.Events.OrderCancelled

import akka.persistence.query._

import scala.concurrent.Future
import scala.language.postfixOps


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

class UserActor extends PersistentActor with ActorLogging {
  
  import poc.persistence.write._
  
  override def persistenceId: String = s"user-${self.path.name}"
  
  var history: String = ""
  
  override def receiveRecover = {
    case e: String => onEvent( e )    
  }

  override def receiveCommand = {
    case e: Event => 
        persist( e.toString() ) { eventSaved => 
            onEvent( eventSaved )
            log.info( "I am {} and it is persistend the following event {}", self.path, eventSaved )                               
        }
    case GetHistory =>      
      log.info( "Get history form user {} ", history )
      sender ! createMsgHistory
  }
 
  
  def onEvent( element : String ){
      history = s"$history\n$element"
      log.info( "New history -> {}", history )
    
  }

   def createMsgHistory : String =   {
s"""
·············
HISTORY $persistenceId:
$history
·············
"""    
  }

}



