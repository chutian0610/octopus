package io.octopus.sql.tree

trait SqlNodeVisitor[R,C] {
  def process(sqlNode: SqlNode, context: C): R

}
