package poc.persistence
package functional
package read

import Poc.ProgramAsync


object UserViewServiceActor {
  
  import akka.actor.{ActorRef, ActorSystem}
  import scala.concurrent.{Future, ExecutionContext}
  
  def apply(implicit ec : ExecutionContext, system : ActorSystem ) = new UserViewService[ProgramAsync] {
    
    import akka.pattern.Patterns
    import poc.persistence.functional.Poc.ProgramAsync
    import poc.persistence.functional.{Error,ErrorActorUser,ErrorActorOrder}
    import poc.persistence.functional.write.OrderEvent
    import poc.persistence.publish.DeliverId
    import poc.persistence.read.UserActorFunctional
    import scala.concurrent.duration._
    import scalaz.{MonadError, MonadReader}
    import scalaz.syntax.monadError._
    import scalaz.syntax._
    import scalaz.{\/-, -\/} 
    
    implicit val E: MonadError[ProgramAsync,Error] = Poc.createMonadError

    val actorUser = UserActorFunctional.receiver
    
    /**
     * 
     * */
    def getHistory( idUser:Long ): poc.persistence.functional.Poc.ProgramAsync[String] = {
      
      import poc.persistence.read.GetHistoryFor
        
      val res0 = Patterns ask ( actorUser, GetHistoryFor(idUser), 20 seconds )
        
      val res : Future[String] = mapToString( res0 )
      
      futureFromProgramAsyncUser( res, idUser )
      
      
    }
    
    /**
     * 
     * */
    def update(orderEvent: OrderEvent): ProgramAsync[Boolean] = {
      
      val res0 = Patterns ask ( actorUser, orderEvent, 20 seconds )
      
      val res : Future[Boolean] = mapToBoolean( res0 )
      
      futureFromProgramAsyncOrder( res, orderEvent.idOrder )
      
      
    }
    
    private def futureFromProgramAsyncUser[T] ( future : Future[T], idUser : Long  ) : ProgramAsync[T] = {
      
      future flatMap{ 
        _.point 
      } recoverWith{
        case e  =>  (ErrorActorUser( idUser, e ): Error).raiseError
      }
      
    }
    
    private def futureFromProgramAsyncOrder[T] ( future : Future[T], idOrder : String  ) : ProgramAsync[T] = {
      
      future flatMap{ 
        _.point 
      } recoverWith{
        case e  =>  (ErrorActorOrder( idOrder, e ): Error).raiseError
      }
      
    }

    /**
     * 
     * */
    private def mapToBoolean( res0 : Future[Any] )(implicit ec: ExecutionContext) : Future[ Boolean ] = {
      import akka.actor.Status._
      res0 map {
          _ match {
              case msg : Boolean => msg
              case msg           =>  { 
                throw new RuntimeException( s"Unknow message $msg" )
              }
          }
      }
  
    }
    
    private def mapToString( res0 : Future[Any] )(implicit ec: ExecutionContext) : Future[ String ] = {
      import akka.actor.Status._
      res0 map {
          _ match {
              case msg : String => msg
              case msg           =>  { 
                throw new RuntimeException( s"Unknow message $msg" )
              }
          }
      }
  
    }
    
  }
      
    
  
    
    
}




