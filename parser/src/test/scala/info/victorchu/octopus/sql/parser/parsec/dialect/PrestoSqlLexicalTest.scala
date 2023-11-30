package info.victorchu.octopus.sql.parser.parsec.dialect

import info.victorchu.octopus.sql.parser.parsec.dialect.PrestoSqlParser
import info.victorchu.octopus.sql.parser.parsec.SqlWhitespace._
import org.scalatest.wordspec.AnyWordSpec
import fastparse._

class PrestoSqlLexicalTest extends AnyWordSpec {

  "PrestoSqlLexical" should {

    "parse upper identifier success " in {
      val Parsed.Success(value, successIndex) = parse("A", PrestoSqlParser.identifier(_))
      assert(value == "A" && successIndex == 1)
    }

    "parse lower case identifier failure " in {
      val f@Parsed.Failure(label, index, extra) = parse("a", PrestoSqlParser.identifier(_))
      assert(label == "" && index == 0 && f.msg == """Position 1:1, found "a"""")
    }

  }
}