package poc.persistence.read

import akka.actor._
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer

object ReadApp extends App {

  implicit val system = ActorSystem("example")

  implicit val mat = ActorMaterializer()

  val query = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  query.eventsByTag("idUser1").runForeach(e => println(e))

}
