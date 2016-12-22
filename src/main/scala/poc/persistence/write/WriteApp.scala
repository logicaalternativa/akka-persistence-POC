package poc.persistence.write

import akka.actor.{ActorSystem, _}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.json4s.{DefaultFormats, jackson}
import poc.persistence.write.commands.{CancelOrder, InitializeOrder}
import akka.pattern.ask
import akka.http.scaladsl.model.StatusCodes._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object WriteApp extends App {

  implicit val system = ActorSystem("example")

  val conf = ConfigFactory.load()

  implicit val actorMaterializer = ActorMaterializer()


  ClusterSharding(system).start(
    typeName = OrderActor.name,
    entityProps = OrderActor.props,
    settings = ClusterShardingSettings(system),
    extractShardId = OrderActor.extractShardId,
    extractEntityId = OrderActor.extractEntityId
  )

  val handler: ActorRef = ClusterSharding(system).shardRegion(OrderActor.name)

  import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats
  implicit val timeout = Timeout(15 seconds)

  val route =
    path("order" / "initialize") {
        post {
          entity(as[InitializeOrder]) {
            (initializeOrderCommand: InitializeOrder) => {
                onComplete(handler ? initializeOrderCommand) {
                  case Success('CommandAccepted) => complete(OK)
                  case Success('CommandRejected) => complete(BadRequest -> Map("message" -> "command rejected"))
                  case Failure(_) => complete(InternalServerError -> Map("message" -> "internal server error"))
                }
              }
            }
          }
        } ~
    path("order" / "cancel") {
      post {
        entity(as[CancelOrder]) {
          cancelOrderCommand: CancelOrder => {
              onComplete(handler ? cancelOrderCommand) {
                case Success('CommandAccepted) => complete(OK)
                case Success('CommandRejected) => complete(BadRequest -> Map("message" -> "command rejected"))
                case Failure(_) => complete(InternalServerError -> Map("message" -> "internal server error"))
              }
            }
          }
        }
      }

  Http().bindAndHandle(route, "localhost", conf.getInt("web.port"))

}

