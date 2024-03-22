package info.victorchu.octopus.sql.tree

import info.victorchu.octopus.sql.parser.SqlNodePosition

/** root SqlNode.
 *
 * @author victor chu
 */
abstract class SqlNode(position: Option[SqlNodePosition]){
  def getChildren: List[SqlNode]

}
