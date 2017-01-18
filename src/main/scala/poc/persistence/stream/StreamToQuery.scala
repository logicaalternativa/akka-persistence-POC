package poc.persistence.stream

import akka.stream.scaladsl.{Sink, Flow, Source, SourceQueueWithComplete}
import akka.stream.{OverflowStrategy, ActorMaterializer, Graph}
import poc.persistence.write.Commands.EnvelopeOrderWithAck
import akka.actor.{ActorSystem,ActorRef}
import akka.NotUsed
import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.Future

sealed trait StreamMsg

package StreamQuery {

  case class OnComplete() extends StreamMsg
   
}



object StreamToQuery {
  
  import akka.actor.Status._
  
  import org.slf4j.{Logger,LoggerFactory}
  
  val logger = LoggerFactory.getLogger( getClass )
  
  def actorStream( implicit system : ActorSystem  ) : ActorRef = {
      
    import poc.persistence.read.UserActor
    
    import StreamQuery._
    
    val  source = Source.actorRef[EnvelopeOrderWithAck]( Int.MaxValue, OverflowStrategy.fail )
    implicit val materializer = ActorMaterializer()
    
    val userActor = UserActor.receiver    
      
    val sink = Sink.actorRef[EnvelopeOrderWithAck](
      userActor,
      OnComplete()
    ) 
    
    source
          .log("StreamToQuery", s => logger.info ("Stream processing, element -> {}", s ) )
          .to( sink )
          .run
           
          
    
  }
  
  
   
   
  
  
}



