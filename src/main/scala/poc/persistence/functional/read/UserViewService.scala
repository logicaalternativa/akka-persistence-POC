package poc.persistence
package functional
package read

import write.OrderEvent

object UserViewService {
  
  def apply[P[_]](implicit UserViewService: UserViewService[P]) = UserViewService

  object Syntax{
    def update[P[_]]( orderEvent:OrderEvent )(implicit userSvc: UserViewService[P])  = userSvc.update( orderEvent )
    def getHistory[P[_]](idUser: Long)(implicit userSvc: UserViewService[P])  = userSvc.getHistory( idUser ) 
  }
  
}

trait UserViewService [P[_]] {
  
  import scalaz.MonadError
  import scalaz.syntax.monadError._
  import scalaz.syntax._
  
  implicit val E: MonadError[P,Error]
  
  // Primitive
  def update( orderEvent:OrderEvent ) : P[Boolean]
  
  def getHistory( idUser : Long ) : P[String]
    
} 
