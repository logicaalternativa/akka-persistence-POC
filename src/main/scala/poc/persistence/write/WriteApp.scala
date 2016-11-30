package poc.persistence.write

import akka.actor._
import poc.persistence._
import scala ._


object WriteApp extends App {
	
  val time = System.nanoTime()

  val system = ActorSystem("example")
  
  val order1_1  = CmdOrder(1, "order1", "idUser1", INIT)
  val order1_2  = CmdOrder(2, "order1", "idUser1", OK)
  
  val order2_1  = CmdOrder(2, "order2", "idUser1", INIT)
  
  val handler = system.actorOf(Props[Handler], "handler")
  
  
  handler ! order1_1
  handler ! order1_2
  handler ! order2_3
 
  val stream = Get
 
 val readUser1 = system.actorOf(Props(classOf[OrderActor], "idUser1"), "user1")
 val readUser2 = system.actorOf(Props(classOf[OrderActor], "idUser2"), "user2")
 
 
  
  
      
  //~ val persistentActor = system.actorOf(Props[ExamplePersistentActor], "persistentActor-4-scala")
  
  //~ persistentActor ! Cmd("foo")
  
  Thread.sleep(1000)
  system.terminate()
}
