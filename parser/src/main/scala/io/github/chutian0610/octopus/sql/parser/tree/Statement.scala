package io.github.chutian0610.octopus.sql.parser.tree

import io.github.chutian0610.octopus.sql.parser.Position

import scala.collection.immutable.List
abstract class Statement(position: Option[Position] = None) extends SqlNode(position) {}


