package poc.persistence.read

import akka.actor._
import akka.persistence.PersistentActor

import akka.persistence.query._

object StreamManager {

  def props = Props[StreamManager]

}

case object GetLastOffsetProc

case class SaveProgress(offset: Offset)

case class ProgressAcknowledged(offset: Offset)

class StreamManager extends PersistentActor with ActorLogging {
  
  var lastOffsetProc: Offset = NoOffset 

  override def receiveRecover: Receive = {
    case ProgressAcknowledged(i) =>
      lastOffsetProc = i
  }

  override def receiveCommand: Receive = {
    case GetLastOffsetProc =>
      log.info( "It going to return the last offset -> {}. I am {} ", lastOffsetProc, self.path )
      sender ! lastOffsetProc
    case SaveProgress(i: Offset) =>
      persist(ProgressAcknowledged(i)) {
        e => {
          lastOffsetProc = e.offset
          //~ sender ! 'Success
        }
      }
  }

  def onEvent(e: AnyRef) = ??? // implement mas tarde

  override def persistenceId: String = "stream-manager"
}


