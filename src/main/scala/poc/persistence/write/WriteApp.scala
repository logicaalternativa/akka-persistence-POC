package poc.persistence.write

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import poc.persistence.write.Commands.{CancelOrder, InitializeOrder}

import scala.concurrent.duration._
import scala.language.postfixOps

object WriteApp extends App {

  val system = ActorSystem("example")

  ClusterSharding(system).start(
    typeName = OrderActor.name,
    entityProps = OrderActor.props,
    settings = ClusterShardingSettings(system),
    extractShardId = OrderActor.extractShardId,
    extractEntityId = OrderActor.extractEntityId
  )

  val handler: ActorRef = ClusterSharding(system).shardRegion(OrderActor.name)

  val randomCommandGenerator = system.actorOf(RandomCommandGenerator.props(handler), "random")
  import system.dispatcher

  system.scheduler.schedule(5 seconds, 10 seconds, randomCommandGenerator, 'SendRandomCommands)


}

object RandomCommandGenerator {

  def props(handler: ActorRef) = Props(classOf[RandomCommandGenerator], handler)

}

class RandomCommandGenerator(handler: ActorRef) extends Actor {

  def receive = {
    case 'SendRandomCommands => {
      val userId = util.Random.nextInt(10)
      val orderId = util.Random.nextInt(20).toString
      util.Random.nextBoolean() match {
        case true => handler ! InitializeOrder(orderId, userId, Map())
        case false => handler ! CancelOrder(orderId, userId)
      }
    }
  }


}



