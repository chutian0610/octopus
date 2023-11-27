package info.victorchu.octopus.sql.parser

import info.victorchu.octopus.sql.parser.SqlParserOption
import info.victorchu.octopus.sql.tree.SqlNode

/**
 * SqlParser: (sql,parserOption) -> sqlNode
 */
trait SqlParser{
  def parse(sql: String, opt: SqlParserOption ): Either[SqlNode,String]
}
