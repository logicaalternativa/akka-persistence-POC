package poc.persistence

import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.cluster._
import akka.pattern.ask
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.stream.ActorMaterializer
import poc.persistence.write._
import poc.persistence.read._

import akka.persistence.query._

import scala.concurrent.Future
import scala.language.postfixOps

import scala.util.{Success, Failure}
import scala.concurrent.Promise

object UtilActorSystem extends App {
  
  import poc.persistence.write.OrderActor
  import poc.persistence.read.UserActor
  
  def starShardingRegions( system : ActorSystem ) : Unit =  {
    
    UserActor.startRegion( system )
    OrderActor.startRegion( system )
      
  }

  def terminate( system : ActorSystem ) : Future[Terminated]= {
    
    import akka.cluster._
    import scala.concurrent.duration._
    
    UserActor.receiver( system ) ! ShardRegion.GracefulShutdown
    OrderActor.receiver( system ) ! ShardRegion.GracefulShutdown
    
    val delay = Duration.create(5, SECONDS)
    
    val promise : Promise[Terminated] = Promise()
    
    system.scheduler.scheduleOnce( delay ) {
        val cluster = Cluster.get( system )
        cluster.registerOnMemberRemoved( systemTerminate( promise, system ) )
        cluster.leave(cluster.selfAddress)
      } ( system.dispatcher )
      
    promise.future
      
  } 
  
  private def systemTerminate( promise : Promise[Terminated] , system : ActorSystem ) : Unit = {
    
    system.terminate().onComplete { 
      case Success(msg) =>
        promise.success( msg )
      case Failure(e) =>
        promise.failure( e )
      
    }( system.dispatcher )
    
  }
  
}


