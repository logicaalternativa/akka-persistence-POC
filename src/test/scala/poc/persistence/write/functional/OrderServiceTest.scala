package poc.persistence.write.functional
import scalaz.\/, \/.{left, right}
import scalaz.{\/-, -\/} 
import scala.concurrent.{Future, ExecutionContext}


object WriteFunctional {
    
  import scalaz.Monad
  import scalaz.MonadError
  import scalaz.syntax.monadError._
  import scalaz.syntax._
  import OrderService._
  
  def flow[P[_] : OrderService : Monad ] ( fromCommand : OrderCommand ) : P[OrderEvent] = {
    
    import OrderService.Syntax._
    
    validateOrder( fromCommand ) >>!
    ( event =>  saveCommand( fromCommand )( event.version ) ) 
    
    //~ for {
      //~ 
      //~ event <- validateOrder( fromCommand )
      //~ newEvent <- saveCommand( fromCommand )( event.version )
      //~ 
    //~ } yield( newEvent )
    
  } 
  
}

object Test {
  
  import WriteFunctional._
  
  import OrderServiceTestAsync._
  import OrderServiceTestSync._
  
  val executeSyncOk    = flow[ ProgramSync ] ( InitOrder( "1", 0 ) )
  val executeSyncNoOk  = flow[ ProgramSync ] ( CloseOrder( "1", 0 ) )
                                               
  val executeAsyncOk   = flow[ ProgramAsync ]( InitOrder( "1", 0 ) )
  val executeAsyncNoOk = flow[ ProgramAsync ]( CloseOrder( "1", 0 ) )
  
}
