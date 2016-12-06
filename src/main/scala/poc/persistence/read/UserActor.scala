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

  private def props = Props[UserActor]

  // the input for the extractShardId function
  // is the message that the "handler" receives
  private def extractShardId: ShardRegion.ExtractShardId = {
    //~ case msg: Event =>
      //~ (msg.timeStamp % 2).toString // <- Be carefull with this. By Timestamp is not good idea
    case _ => "1"
  }

  // the input for th extractEntityId function
  // is the message that the "handler" receives
  private def  extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: OrderInitialized =>
      (msg.order.idUser.toString, msg)
    case msg: OrderCancelled =>
      (msg.order.idUser.toString, msg)
    case msg: GetHistoryFor =>
      (msg.idUser.toString, GetHistory)

  }
  
  def startRegion( system: ActorSystem ) {
    
    ClusterSharding(system).start(
      typeName = name,
      entityProps = props,
      settings = ClusterShardingSettings(system),
      extractShardId = extractShardId,
      extractEntityId = extractEntityId
    )
    
  }
  def handlerForUsers( system : ActorSystem ) : ActorRef = {
    ClusterSharding( system )
    .shardRegion( name )
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
        log.info( "It is recived the following command {}. I am {} ", e, self.path )       
        persist( e.toString() ) { eventSaved =>
            log.info( "It is persisted the following event {}", eventSaved )       
            onEvent( eventSaved )                         
        }
    case GetHistory =>      
      log.info( "Get history from user {} ", history )
      sender ! createMsgHistory
    
  }
 
  
  def onEvent( element : String ){
      history = s"$history\n$element"
      log.info( "It is added this element to history -> {}", element )
      log.info( "Now, history is -> {}", history )
    
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



