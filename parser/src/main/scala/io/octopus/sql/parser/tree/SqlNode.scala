package io.octopus.sql.parser.tree

import io.octopus.sql.parser.Position
import io.octopus.sql.utils.Engine

import java.util.Locale

/**
 * Root Sql Node.
 *
 * @param position sql position
 */
abstract class SqlNode(position: Option[Position] = None) {
  /**
   * get children Sql Node
   *
   * @return
   */
  def getChildren: List[SqlNode]
}

