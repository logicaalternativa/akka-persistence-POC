package poc.persistence.write

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}

object WriteApp extends App {
	
  import Commands._

  val system = ActorSystem("example")

  ClusterSharding(system).start(
    typeName = OrderActor.name, // orders
    entityProps = OrderActor.props,
    settings = ClusterShardingSettings(system),
    extractShardId = OrderActor.extractShardId,
    extractEntityId = OrderActor.extractEntityId
  )

  val handler: ActorRef = ClusterSharding(system).shardRegion(OrderActor.name)

  val order1Init = InitializeOrder(1, "order1", 1)
  val order1Cancel = CancelOrder(2, "order1", 1)

  handler ! order1Init
  handler ! order1Cancel
  handler ! order1Cancel // will be rejected!

}
