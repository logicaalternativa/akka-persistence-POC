package poc.persistence.write

import org.json4s.DefaultFormats
import org.json4s.jackson._
import org.scalatest.FunSuite
import poc.persistence.write.commands.InitializeOrder

class Test extends FunSuite {

  test("deserialization of InitializeOrder") {
    val data = """{"idOrder":"1", "idUser": 42, "orderData": "something" }"""
    implicit val formats = DefaultFormats
    val result: InitializeOrder = Serialization.read[InitializeOrder](data)
    assert(result.idOrder == "1")
    assert(result.idUser == 42)
  }

}
