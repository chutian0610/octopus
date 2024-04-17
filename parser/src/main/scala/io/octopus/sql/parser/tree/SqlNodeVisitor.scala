package io.octopus.sql.parser.tree

trait SqlNodeVisitor[R,C] {
  def process(sqlNode: SqlNode, context: C): R

}
