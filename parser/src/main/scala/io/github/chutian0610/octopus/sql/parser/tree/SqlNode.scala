package io.github.chutian0610.octopus.sql.parser.tree

import io.github.chutian0610.octopus.sql.parser.Position

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

