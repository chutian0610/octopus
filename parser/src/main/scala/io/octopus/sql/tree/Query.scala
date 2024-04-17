package io.octopus.sql.tree

import io.octopus.sql.parser.SqlNodePosition

case class Query(position: Option[SqlNodePosition] = None,
                 withClause: Option[WithClause] = None,
                 queryBody: QueryBody,
                 orderBy: Option[OrderBy] = None,
                 limit: Option[Limit] = None,
                 offset: Option[Offset] = None,
                ) extends Statement(position) {
  override def getChildren: List[SqlNode] = {
    List(withClause, Some(queryBody), orderBy, limit, offset).flatten
  }
}

case class WithClause(position: Option[SqlNodePosition] = None,
                      withQueries: List[WithQuery],
                      recursive: Boolean = false
                     ) extends SqlNode(position) {

  override def getChildren: List[SqlNode] = withQueries
}

case class WithQuery(position: Option[SqlNodePosition] = None,
                     name: Identifier,
                     query: Query,
                     columnNames: Option[List[Identifier]] = None
                    ) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = List(query)
}


case class Select(position: Option[SqlNodePosition] = None,
                  distinct: Boolean = false,
                  selectItems: List[SelectItem]
                 ) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = selectItems
}

abstract class SelectItem(position: Option[SqlNodePosition] = None) extends SqlNode(position) {}

case class SingleColumn(position: Option[SqlNodePosition] = None,
                        expression: Expression,
                        alias: Option[Identifier] = None
                       ) extends SelectItem(position) {
  override def getChildren: List[SqlNode] = List(expression)
}

case class AllColumns(position: Option[SqlNodePosition] = None,
                      prefix: Option[QualifiedName] = None
                     ) extends SelectItem(position) {
  override def getChildren: List[SqlNode] = List.empty
}

case class GroupBy(position: Option[SqlNodePosition] = None,
                   distinct: Boolean = false,
                   groupingElements: List[GroupingElement]
                  ) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = groupingElements
}

abstract class GroupingElement(position: Option[SqlNodePosition] = None) extends SqlNode(position) {
  def getExpressions: List[Expression]
}

case class GroupingSets(position: Option[SqlNodePosition] = None,
                        sets: List[List[Expression]]) extends GroupingElement(position) {
  override def getChildren: List[SqlNode] = List.empty

  override def getExpressions: List[Expression] = sets.flatten
}

case class Cube(position: Option[SqlNodePosition] = None, columns: List[Expression]) extends GroupingElement(position) {
  override def getChildren: List[SqlNode] = List.empty

  override def getExpressions: List[Expression] = columns
}

case class Rollup(position: Option[SqlNodePosition] = None, columns: List[Expression]) extends GroupingElement(position) {
  override def getChildren: List[SqlNode] = List.empty

  override def getExpressions: List[Expression] = columns
}

case class SimpleGroupBy(position: Option[SqlNodePosition] = None,
                         columns: List[Expression]) extends GroupingElement(position) {
  override def getChildren: List[SqlNode] = columns

  override def getExpressions: List[Expression] = columns
}

case class OrderBy(position: Option[SqlNodePosition] = None,
                   sortItems: List[SortItem]) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = sortItems
}

enum Ordering {
  case ASCENDING, DESCENDING
}

enum NullOrdering {
  case FIRST, LAST, UNDEFINED
}

case class SortItem(position: Option[SqlNodePosition] = None,
                    sortKey: Expression,
                    ordering: Ordering,
                    nullOrdering: NullOrdering) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = List(sortKey)
}


case class Limit(position: Option[SqlNodePosition] = None,
                 count: String) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = List.empty

}

case class Offset(position: Option[SqlNodePosition] = None,
                  count: String) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = List.empty
}
