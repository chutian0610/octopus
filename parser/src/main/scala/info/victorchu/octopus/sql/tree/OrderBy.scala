package info.victorchu.octopus.sql.tree

import info.victorchu.octopus.sql.parser.SqlNodePosition

class OrderBy(position: Option[SqlNodePosition]) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = ???
}
