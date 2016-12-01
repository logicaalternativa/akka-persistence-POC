package poc.persistence.write

import akka.actor._
import poc.persistence._
import scala ._


object WriteApp extends App {
	
  import Commands._
	
  val time = System.nanoTime()

  val system = ActorSystem("example")
  
  val order1Init  = InitializeOrder(1, "order1", "idUser1")
  val order1Cancel  = CancelOrder(2, "order1", "idUser1")
  
  val handler = system.actorOf(Props[Handler], "handler")
  
  handler ! order1Init
  handler ! order1Cancel
  handler ! order1Cancel
 
 //~ val stream = Get
 
 //~ val readUser1 = system.actorOf(Props(classOf[OrderActor], "idUser1"), "user1")
 //~ val readUser2 = system.actorOf(Props(classOf[OrderActor], "idUser2"), "user2")
     
  //~ val persistentActor = system.actorOf(Props[ExamplePersistentActor], "persistentActor-4-scala")
  
  //~ persistentActor ! Cmd("foo")
  
  //~ Thread.sleep(1000)
  //~ system.terminate()
}
