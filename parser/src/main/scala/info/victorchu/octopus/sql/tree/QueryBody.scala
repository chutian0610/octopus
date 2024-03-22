package info.victorchu.octopus.sql.tree

import info.victorchu.octopus.sql.parser.SqlNodePosition

abstract class QueryBody(position: Option[SqlNodePosition]) extends Relation(position) {

}
