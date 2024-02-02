package info.victorchu.octopus.sql.parser.antlr

import info.victorchu.octopus.sql.parser.antlr.presto.{PrestoAntlr4Printer, PrestoParsingOptions, PrestoSqlParser}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.Assertions.*
class PrestoSqlParserTest extends AnyWordSpec {

  "PrestoSqlParser" should {
    "parse success " in {
      val sql =
        """SELECT productid, name
          | FROM   product
          | ORDER  BY productid
          | LIMIT  3""".stripMargin
      val statement = PrestoSqlParser().createStatement(sql)
      new PrestoAntlr4Printer().visit(statement)
    }
  }
}