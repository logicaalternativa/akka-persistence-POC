package poc.persistence.read

import akka.actor._
import poc.persistence._
import scala ._


object ReadApp extends App {
	
  val time = System.nanoTime()

  val system = ActorSystem("example")
  
  
  //~ val persistentActor = system.actorOf(Props[ExamplePersistentActor], "persistentActor-4-scala")
  
  //~ persistentActor ! Cmd("foo")
  
  Thread.sleep(1000)
  system.terminate()
}
