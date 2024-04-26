package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.SqlDialect
import io.octopus.sql.parser.tree.SqlNode

/**
 * SqlParser: (sql,parserOption) -> Either[SqlParsingException, SqlNode]
 */
class SqlParser(sqlDialect: SqlDialect,sqlParingOption: SqlParingOption = SqlParingOption()) {
  val tokenizer: SqlTokenizer = new SqlTokenizer(sqlDialect, sqlParingOption)

  def parse(sql: String): Either[SqlParsingException, SqlNode]= {
    ???
  }
}
