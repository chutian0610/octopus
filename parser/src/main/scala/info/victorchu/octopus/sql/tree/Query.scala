package info.victorchu.octopus.sql.tree

import info.victorchu.octopus.sql.parser.SqlNodePosition

class Query(position: Option[SqlNodePosition],
            withClause: Option[WithClause],
            queryBody: QueryBody,
            orderBy: Option[OrderBy],
            limit: Option[Limit],
            offset: Option[Offset],
           ) extends Statement(position) {

  override def getChildren: List[SqlNode] = List()
}
