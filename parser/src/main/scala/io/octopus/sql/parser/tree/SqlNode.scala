package io.octopus.sql.parser.tree

import io.octopus.sql.parser.Position

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

