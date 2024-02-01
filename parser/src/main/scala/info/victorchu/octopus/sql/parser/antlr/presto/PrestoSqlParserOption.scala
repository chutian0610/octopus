package info.victorchu.octopus.sql.parser.antlr.presto

import info.victorchu.octopus.sql.parser.SqlParserOption
case class PrestoSqlParserOption(
                             allowedIdentifierSymbols:Set[String],
                             enhancedErrorHandlerEnabled:Boolean
                           ) extends SqlParserOption{

}

object PrestoSqlParserOption{
  def apply():PrestoSqlParserOption={
    new PrestoSqlParserOption(Set(),true)
  }
}
