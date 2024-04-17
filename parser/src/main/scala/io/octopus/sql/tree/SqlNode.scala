package io.octopus.sql.tree

import io.octopus.sql.parser.SqlNodePosition
import io.octopus.sql.utils.Engine

import java.util.Locale

/**
 * Root Sql Node.
 *
 * @param position sql position
 */
abstract class SqlNode(position: Option[SqlNodePosition] = None) {
  /**
   * get children Sql Node
   *
   * @return
   */
  def getChildren: List[SqlNode]
}

