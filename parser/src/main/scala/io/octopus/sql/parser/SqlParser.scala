package io.octopus.sql.parser

import io.octopus.sql.parser.tree.SqlNode

/**
 * SqlParser: (sql,parserOption) -> Either[SqlParsingException, SqlNode]
 */
class SqlParser(opt: SqlParserOption) {
  def parse(sql: String): Either[SqlParsingException, SqlNode]={
    ???
  }
}
