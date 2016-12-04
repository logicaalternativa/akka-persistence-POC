package poc.persistence.write

import akka.actor._
import akka.pattern.ask

import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import scala.util.{Success, Failure}

object WriteApp extends App {
  
  import Commands._

  val system = ActorSystem("example")
  
  import scala.concurrent.duration._
  implicit val timeout = akka.util.Timeout(10 seconds)
  
  import system.dispatcher 
  import akka.pattern.ask

  ClusterSharding(system).start(
    typeName = OrderActor.name, // orders
    entityProps = OrderActor.props,
    settings = ClusterShardingSettings(system),
    extractShardId = OrderActor.extractShardId,
    extractEntityId = OrderActor.extractEntityId
  )

  val handler: ActorRef = ClusterSharding(system).shardRegion(OrderActor.name)

  
  // Testing
  val order1Init = InitializeOrder(1, "order1", 1)
  val order1Cancel = CancelOrder(2, "order1", 1)

  handler ! order1Init
  handler ! order1Cancel
  val resFuture = ( handler ? order1Cancel ) // will be rejected!
  
  resFuture.onComplete {
    case Success(msg) =>
      println(s"********* Result sending two times order cancel -> $msg *********")
    case Failure(e) =>
      println(s"********* Info from the exception:  ${e.getMessage} *********")
  }

}
