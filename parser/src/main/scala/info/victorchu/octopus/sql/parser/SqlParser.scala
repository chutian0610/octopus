package info.victorchu.octopus.sql.parser

import info.victorchu.octopus.sql.tree.SqlNode

/**
 * SqlParser: (sql,parserOption) -> sqlNode
 */
trait SqlParser(opt: SqlParserOption){
  def parse(sql: String): Either[SqlNode,String]
}
