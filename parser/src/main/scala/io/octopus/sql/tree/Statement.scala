package io.octopus.sql.tree

import io.octopus.sql.parser.SqlNodePosition

import scala.collection.immutable.List
abstract class Statement(position: Option[SqlNodePosition] = None) extends SqlNode(position) {}


