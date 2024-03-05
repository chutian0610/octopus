package info.victorchu.octopus.sql.parser.antlr.presto

import info.victorchu.octopus.sql.parser.SqlParserOption

enum DecimalLiteralTreatment {
  case AS_DOUBLE, AS_DECIMAL, REJECT
}
case class PrestoSqlParserOption(
                             allowedIdentifierSymbols:Set[String],
                             enhancedErrorHandlerEnabled:Boolean,
                             decimalLiteralTreatment:DecimalLiteralTreatment
                           ) extends SqlParserOption{

}

object PrestoSqlParserOption{
  def apply():PrestoSqlParserOption={
    new PrestoSqlParserOption(Set(),true,DecimalLiteralTreatment.AS_DECIMAL)
  }

  def apply(allowedIdentifierSymbols: Set[String]): PrestoSqlParserOption = {
    new PrestoSqlParserOption(Set(), true, DecimalLiteralTreatment.AS_DECIMAL)
  }
  def apply(allowedIdentifierSymbols:Set[String],decimalLiteralTreatment:DecimalLiteralTreatment): PrestoSqlParserOption = {
    new PrestoSqlParserOption(Set(), true, decimalLiteralTreatment)
  }
}
