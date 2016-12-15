package poc.persistence.read.streammanager

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import poc.persistence.read.streammanager.commands.SaveProgress
import poc.persistence.read.streammanager.events.ProgressAcknowledged
import poc.persistence.read.streammanager.queries.GetLastOffsetProcessed

object StreamManager {
  def props = Props[StreamManager]
}

class StreamManager extends PersistentActor with ActorLogging {

  override def persistenceId: String = "stream-manager"

  var lastOffsetProc: Long = 0L // initial value is 0

  override def receiveRecover: Receive = {
    case ProgressAcknowledged(i) =>
      lastOffsetProc = i
  }

  override def receiveCommand: Receive = {
    case GetLastOffsetProcessed =>
      sender ! lastOffsetProc
    case SaveProgress(i: Long) =>
      persist(ProgressAcknowledged(i)) {
        e => {
          lastOffsetProc = e.i
          sender ! 'Success
        }
      }
  }

}