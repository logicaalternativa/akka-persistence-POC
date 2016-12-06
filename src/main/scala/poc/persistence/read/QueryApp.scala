package poc.persistence.read

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.ask
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.stream.ActorMaterializer

import akka.persistence.query._

import scala.concurrent.Future
import scala.language.postfixOps

object QueryApp extends App {

  import scala.concurrent.duration._
  implicit val timeout = akka.util.Timeout(10 seconds)
  import java.util.UUID

  implicit val system = ActorSystem("example")
  import system.dispatcher

  UserActor.startRegion( system )
  
  val handlerForUsers: ActorRef = UserActor.handlerForUsers( system )

  (handlerForUsers ? GetHistoryFor(1)).onSuccess {
    case s => println(s)
  }
}


