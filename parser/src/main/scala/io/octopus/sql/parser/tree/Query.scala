package io.octopus.sql.parser.tree

import io.octopus.sql.parser.Position

case class Query(position: Option[Position] = None,
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

case class WithClause(position: Option[Position] = None,
                      recursive: Boolean = false,
                      withQueries: List[WithQuery]
                     ) extends SqlNode(position) {

  override def getChildren: List[SqlNode] = withQueries
}

case class WithQuery(position: Option[Position] = None,
                     name: Identifier,
                     columnNames: Option[List[Identifier]] = None,
                     query: Query
                    ) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = List(query)
}


case class Select(position: Option[Position] = None,
                  distinct: Boolean = false,
                  selectItems: List[SelectItem]
                 ) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = selectItems
}

abstract class SelectItem(position: Option[Position] = None) extends SqlNode(position) {}

case class SingleColumn(position: Option[Position] = None,
                        expression: Expression,
                        alias: Option[Identifier] = None
                       ) extends SelectItem(position) {
  override def getChildren: List[SqlNode] = List(expression)
}

case class AllColumns(position: Option[Position] = None,
                      prefix: Option[QualifiedName] = None
                     ) extends SelectItem(position) {
  override def getChildren: List[SqlNode] = List.empty
}

case class GroupBy(position: Option[Position] = None,
                   distinct: Boolean = false,
                   groupingElements: List[GroupingElement]
                  ) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = groupingElements
}

abstract class GroupingElement(position: Option[Position] = None) extends SqlNode(position) {
  def getExpressions: List[Expression]
}

case class GroupingSets(position: Option[Position] = None,
                        sets: List[List[Expression]]) extends GroupingElement(position) {
  override def getChildren: List[SqlNode] = List.empty

  override def getExpressions: List[Expression] = sets.flatten
}

case class Cube(position: Option[Position] = None, columns: List[Expression]) extends GroupingElement(position) {
  override def getChildren: List[SqlNode] = List.empty

  override def getExpressions: List[Expression] = columns
}

case class Rollup(position: Option[Position] = None, columns: List[Expression]) extends GroupingElement(position) {
  override def getChildren: List[SqlNode] = List.empty

  override def getExpressions: List[Expression] = columns
}

case class SimpleGroupBy(position: Option[Position] = None,
                         columns: List[Expression]) extends GroupingElement(position) {
  override def getChildren: List[SqlNode] = columns

  override def getExpressions: List[Expression] = columns
}

case class OrderBy(position: Option[Position] = None,
                   sortItems: List[SortItem]) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = sortItems
}

enum Ordering {
  case ASCENDING, DESCENDING
}

enum NullOrdering {
  case FIRST, LAST, UNDEFINED
}

case class SortItem(position: Option[Position] = None,
                    sortKey: Expression,
                    ordering: Ordering,
                    nullOrdering: NullOrdering) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = List(sortKey)
}


case class Limit(position: Option[Position] = None,
                 count: String) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = List.empty

}

case class Offset(position: Option[Position] = None,
                  count: String) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = List.empty
}
