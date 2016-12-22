package poc.persistence.read.streammanager.commands

sealed trait Command
case class SaveProgress(i: Long) extends Command