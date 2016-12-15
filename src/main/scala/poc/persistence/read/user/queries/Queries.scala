package poc.persistence.read.user.queries

import poc.persistence.read.user.events

sealed trait Query

case object GetHistory extends Query

case class GetHistoryFor(idUser: Long) extends Query

sealed trait Response

case class History(history: List[(String, events.UserEvent)]) extends Response