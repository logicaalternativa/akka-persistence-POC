package poc.persistence.read.streammanager.events

sealed trait Event
case class ProgressAcknowledged(i: Long) extends Event
