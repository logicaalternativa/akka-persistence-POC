package poc.persistence
package write

import akka.actor._
import akka.pattern.ask

import scala.util.{Success, Failure}
import poc.persistence.functional.Error
import poc.persistence.functional.write.{OrderCommand, InitiatedOrder}


object TestApp extends App {

  import poc.persistence.functional.Poc.streamActor
  import poc.persistence.functional.Poc.implicits._
  import poc.persistence.functional.Poc.ProgramAsync
  import poc.persistence.functional.ApplicationFlow
  
   // Traza
   
   println( s"Llamando al programa  streamActor -> ${streamActor.path}" )
   
   // Fin de traza
   
   scala.io.StdIn.readLong
   
   
  import poc.persistence.functional.write.InitOrder
  //~ import poc.persistence.publish.OrderEventWithDeliver
   
   
   //~ val res = userViewServiceActor.update( InitiatedOrder( "order1", 1,0) )
   //~ val res = WriteProgram.toView[ProgramAsync]( OrderEventWithDeliver(1, InitiatedOrder( "order1", 1,0)) )
   val res = ApplicationFlow.write[ProgramAsync]( InitOrder("order1", 1) )
   
   res onComplete( _ match {
        case Success( s )  =>  {
            
             s.fold( 
              error => println(s"SALIDA NoK => $error") , 
              value => println(s"SALIDA OK => $value")
            ) 
          
        }
        case  Failure( e ) =>  println(s"SALIDA ERROR =>  $e.getMessage") 
      }
    )
   
}
