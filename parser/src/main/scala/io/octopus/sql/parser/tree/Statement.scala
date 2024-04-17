package io.octopus.sql.parser.tree

import io.octopus.sql.parser.Position

import scala.collection.immutable.List
abstract class Statement(position: Option[Position] = None) extends SqlNode(position) {}


