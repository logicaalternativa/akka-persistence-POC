package poc.persistence.read.streammanager.queries

sealed trait Query
case object GetLastOffsetProcessed extends Query
