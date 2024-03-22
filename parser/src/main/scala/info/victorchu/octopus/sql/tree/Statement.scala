package info.victorchu.octopus.sql.tree

import info.victorchu.octopus.sql.parser.SqlNodePosition

abstract class Statement(position: Option[SqlNodePosition]) extends SqlNode(position) {

}
