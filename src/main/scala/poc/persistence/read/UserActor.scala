package poc.persistence.read

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.stream.ActorMaterializer
import poc.persistence.write.Events.OrderCancelled

import akka.persistence.query._
import poc.persistence.BaseObjectActor

import scala.concurrent.Future
import scala.language.postfixOps


import poc.persistence.write.Event
import poc.persistence.write.Events.OrderInitialized
import poc.persistence.write.Commands.EnvelopeOrderWithAck

sealed trait Query

case object GetHistory extends Query

case class GetHistoryFor(idUser:Long) extends Query

object UserActor extends BaseObjectActor{

  def name = "Users"

  protected def props( implicit system : ActorSystem ) = Props[UserActor]

  // the input for the extractShardId function
  // is the message that the "handler" receives
  protected def extractShardId: ShardRegion.ExtractShardId = {
    //~ case msg: Event =>
      //~ (msg.timeStamp % 2).toString // <- Be carefull with this. By Timestamp is not good idea
    case _ => "1"
  }

  // the input for th extractEntityId function
  // is the message that the "handler" receives
  protected def  extractEntityId: ShardRegion.ExtractEntityId = {
    case msg : EnvelopeOrderWithAck =>
        msg.event match  {
        case initalized : OrderInitialized  => 
          (initalized.order.idUser.toString, msg)
        case cancelled : OrderCancelled => 
          (cancelled.order.idUser.toString, msg)
        }
      case msg: GetHistoryFor =>
      (msg.idUser.toString, GetHistory)

  }
  
  
  
}

class UserActor extends PersistentActor with ActorLogging {
  
  import poc.persistence.write._
  import poc.persistence.stream.StreamQuery._
  
  
  override def persistenceId: String = s"user-${self.path.name}"
  
  var history: String = ""
  
  override def receiveRecover = {
    case e: String => onEvent( e )    
  }

  override def receiveCommand = {
    
    case EnvelopeOrderWithAck( ackMsg, actorRefSender, e ) => {
        
        log.info( "It is recived the following command {}. I am {}. The sender is {} ", e, self.path, sender )       
        
        persist( e.toString() ) { eventSaved =>
            log.info( "It is persisted the following event {}", eventSaved )       
            onEvent( eventSaved )
            actorRefSender ! ackMsg
            log.info( "Sent ack msg {} to ActorRef: {}", ackMsg, actorRefSender )                        
        }
      
    }
    
    case GetHistory =>      
      log.info( "Get history from user {} ", history )
      sender ! createMsgHistory
    
    case msg => log.info ("Unknown message {}", msg)
    
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



