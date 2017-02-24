package poc.persistence.functional

package publish

import write.OrderEvent

object PublishService {
  
  def apply[P[_]](implicit PublishService: PublishService[P]) = PublishService

  object Syntax{
    def publish[P[_]]( orderEvent:OrderEvent )(implicit publishSvc: PublishService[P])  = publishSvc.publish( orderEvent )
    def sendAck[P[_]](idMessage: Long, idOrder : String, version : Long)(implicit publishSvc: PublishService[P])  = publishSvc.sendAck( idMessage, idOrder, version  ) 
  }
  
}

trait PublishService [P[_]] {
  
  import scalaz.MonadError
  import scalaz.syntax.monadError._
  import scalaz.syntax._
  
  implicit val E: MonadError[P,Error]
  
  // Primitive
  def publish( orderEvent:OrderEvent ) : P[Long]
  
  def sendAck(idMessage: Long, idOrder : String, version : Long) : P[Boolean]
    
} 
