package info.victorchu.octopus.sql.parser.antlr

class PrestoSqlParserTest extends AnyWordSpec {

  "PrestoSqlParser" should {
    "parse success " in {
      val sql =
        """SELECT productid, name
          | FROM   product
          | ORDER  BY productid
          | LIMIT  3""".stripMargin
      val statement = PrestoSqlParser().createStatement(sql, PrestoParsingOptions(PrestoParsingOptions.DecimalLiteralTreatment.AS_DOUBLE))
    }
  }
}