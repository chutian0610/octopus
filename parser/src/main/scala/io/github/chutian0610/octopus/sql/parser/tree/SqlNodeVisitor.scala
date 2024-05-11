package io.github.chutian0610.octopus.sql.parser.tree

trait SqlNodeVisitor[R,C] {
  def process(sqlNode: SqlNode, context: C): R

}
