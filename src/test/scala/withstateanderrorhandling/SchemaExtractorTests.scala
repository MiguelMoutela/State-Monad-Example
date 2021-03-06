package withstateanderrorhandling

import cats.implicits._
import org.scalatest.FunSuite
import play.api.libs.json.Json

class SchemaExtractorTests extends FunSuite {
  test("test schema extractor with state and error handling") {
    val json =
      """{
        |  "value1": 42,
        |  "value2": { "value2.1": "", "value2.2": 5, "value2.3": { "value2.3.1": 1.0, "value2.3.2": 1.0, "value2.3.3": 1.0 } },
        |  "value3": { "value3.1": true, "value3.2": 2 },
        |  "value4": [
        |     [ { "value4.1": "", "value4.2": 123 }, { "value4.1": "", "value4.2": 345 } ],
        |     [ { "value4.1": "", "value4.2": 678 }, { "value4.1": "", "value4.2": 312 } ]
        |  ]
        |}
      """.stripMargin

    val actual = SchemaExtractor.fromJsRoot(Json.parse(json)).runA(0)

    val value4 = SchemaArray(
      SchemaArray(SchemaObject(3, "value4", List(("value4.1", SchemaString), ("value4.2", SchemaDouble)))))
    val value3 = SchemaObject(2, "value3", List(("value3.1", SchemaBoolean), ("value3.2", SchemaDouble)))
    val value2 = SchemaObject(1, "value2", List(
      ("value2.1", SchemaString),
      ("value2.2", SchemaDouble),
      ("value2.3", SchemaObject(0, "value2.3", List(
        ("value2.3.1", SchemaDouble),
        ("value2.3.2", SchemaDouble),
        ("value2.3.3", SchemaDouble)))
      )
    ))

    val expected = SchemaObject(7, "root", List(
      ("value1", SchemaDouble),
      ("value2", value2),
      ("value3", value3),
      ("value4", value4)))

    assert(actual == Right(expected))
  }

  test("test invalid root") {
    val json ="""[1, 2, 3]"""

    val actual = SchemaExtractor.fromJsRoot(Json.parse(json)).runA(0)

    assert(actual == Left("JSON root value must be an object"))
  }

  test("test null value") {
    val json ="""{ "x": null }"""

    val actual = SchemaExtractor.fromJsRoot(Json.parse(json)).runA(0)

    assert(actual == Left("cannot analyze type of a JSON null value"))
  }

  test("test empty array") {
    val json ="""{ "x": 123, "y": [] }"""

    val actual = SchemaExtractor.fromJsRoot(Json.parse(json)).runA(0)

    assert(actual == Left("cannot analyze empty JSON array"))
  }

  test("test inconsistent array") {
    val json ="""{ "x": 123, "y": [ 1, true ] }"""

    val actual = SchemaExtractor.fromJsRoot(Json.parse(json)).runA(0)

    assert(actual == Left("array type is not consistent"))
  }
}
