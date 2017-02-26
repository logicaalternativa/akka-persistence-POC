package poc.persistence.read

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.ask
import akka.persistence.PersistentActor

import akka.persistence.query._
import poc.persistence.BaseObjectActor

import scala.concurrent.Future
import scala.language.postfixOps


import poc.persistence.write.{Event,WithUser}

sealed trait Query

case object GetHistory extends Query
case class GetHistoryFor(idUser:Long) extends Query


object UserActorFunctional extends BaseObjectActor{

  def name = "Users"

  protected def props( implicit system : ActorSystem ) = Props[UserActorFunctional]

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
    case msg : WithUser =>
        (msg.idUser.toString, msg)
      case msg: GetHistoryFor =>
      (msg.idUser.toString, GetHistory)

  }
  
  
  
}

class UserActorFunctional extends PersistentActor with ActorLogging {
  
  import poc.persistence.write._
  import poc.persistence.functional.write.OrderEvent
  
  
  override def persistenceId: String = s"user-${self.path.name}"
  
  var history: String = ""
  
  override def receiveRecover = {
    case e: String => onEvent( e )    
  }

  override def receiveCommand = {
    
    case o: OrderEvent => {
      
        log.info( "It is recived the following command {}. I am {}.", o, self.path )       
        
        persist( o.toString() ) { eventSaved =>
            log.info( "It is persisted the following event {}", eventSaved )       
            onEvent( eventSaved )
            sender ! Status.Success( true )
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



