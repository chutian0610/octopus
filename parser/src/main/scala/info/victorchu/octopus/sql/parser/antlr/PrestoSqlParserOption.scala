package info.victorchu.octopus.sql.parser.antlr

import info.victorchu.octopus.sql.parser.SqlParserOption
case class PrestoSqlParserOption(
                             allowedIdentifierSymbols:Set[String]= Set[String](),
                             enhancedErrorHandlerEnabled:Boolean
                           ) extends SqlParserOption{

}
