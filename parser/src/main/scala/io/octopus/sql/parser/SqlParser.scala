package io.octopus.sql.parser

import io.octopus.sql.tree.SqlNode

/**
 * SqlParser: (sql,parserOption) -> sqlNode
 */
trait SqlParser(opt: SqlParserOption) {
  def parse(sql: String): Either[String, SqlNode]
}
