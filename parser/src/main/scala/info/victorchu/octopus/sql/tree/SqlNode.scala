package info.victorchu.octopus.sql.tree

import info.victorchu.octopus.sql.parser.SqlNodePosition

/** root SqlNode.
  *
  * @author victor chu
  */
trait SqlNode(position: Option[SqlNodePosition])
