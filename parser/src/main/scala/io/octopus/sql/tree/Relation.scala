package io.octopus.sql.tree

import io.octopus.sql.parser.SqlNodePosition

import scala.collection.immutable.List

abstract class Relation(position: Option[SqlNodePosition] = None) extends SqlNode(position) {}


case class AliasedRelation(position: Option[SqlNodePosition] = None,
                           relation: Relation,
                           alias: Identifier,
                           columnAliases: List[Identifier]) extends Relation(position) {
  override def getChildren: List[SqlNode] = List(relation)
}

enum SampledType {
  case BERNOULLI, SYSTEM
}

case class SampledRelation(position: Option[SqlNodePosition] = None,
                           relation: Relation,
                           sampleType: SampledType,
                           samplePercentage: Expression
                          ) extends Relation(position) {
  override def getChildren: List[SqlNode] = List(relation, samplePercentage)
}

case class Unnest(position: Option[SqlNodePosition] = None,
                  expressions: List[Expression],
                  withOrdinality: Boolean = false) extends Relation(position) {
  override def getChildren: List[SqlNode] = expressions
}

case class Lateral(position: Option[SqlNodePosition] = None,
                   query: Query) extends Relation(position) {
  override def getChildren: List[SqlNode] = List(query)
}

abstract class QueryBody(position: Option[SqlNodePosition] = None) extends Relation(position) {}


case class QuerySpecification(position: Option[SqlNodePosition] = None,
                              select: Select,
                              from: Option[Relation] = None,
                              where: Option[Expression] = None,
                              groupBy: Option[GroupBy] = None,
                              having: Option[Expression] = None,
                              orderBy: Option[OrderBy] = None,
                              limit: Option[Limit] = None,
                              offset: Option[Offset] = None,
                             ) extends QueryBody(position) {
  override def getChildren: List[SqlNode] = List.empty
}

case class SubQuery(position: Option[SqlNodePosition] = None,
                    query: Query
                   ) extends QueryBody(position) {
  override def getChildren: List[SqlNode] = List(query)
}

case class Table(position: Option[SqlNodePosition] = None,
                 name: QualifiedName
                ) extends QueryBody(position) {
  override def getChildren: List[SqlNode] = List.empty
}

case class Values(position: Option[SqlNodePosition] = None,
                  rows: List[Expression]
                 ) extends QueryBody(position) {
  override def getChildren: List[SqlNode] = rows
}

enum JoinType {
  case CROSS, INNER, LEFT, RIGHT, FULL, IMPLICIT
}

case class Join(position: Option[SqlNodePosition] = None,
                joinType: JoinType,
                left: Relation,
                right: Relation,
                criteria: Option[JoinCriteria] = None,
               ) extends Relation(position) {
  override def getChildren: List[SqlNode] = {
    left :: right :: criteria.toList
  }
}

abstract class JoinCriteria(position: Option[SqlNodePosition] = None) extends SqlNode(position)

case class JoinOn(position: Option[SqlNodePosition] = None, expression: Expression) extends JoinCriteria(position) {
  override def getChildren: List[SqlNode] = List(expression)
}

case class JoinUsing(position: Option[SqlNodePosition] = None, columns: List[Identifier]) extends JoinCriteria(position) {
  override def getChildren: List[SqlNode] = List.empty
}

case class NaturalJoin(position: Option[SqlNodePosition] = None) extends JoinCriteria(position) {
  override def getChildren: List[SqlNode] = List.empty
}

abstract class SetOperation(position: Option[SqlNodePosition] = None, distinct: Option[Boolean] = None) extends QueryBody(position) {
  def getRelations: List[Relation]
}

case class Union(position: Option[SqlNodePosition] = None,
                 relations: List[Relation],
                 distinct: Option[Boolean] = None
                ) extends SetOperation(position, distinct) {
  override def getChildren: List[SqlNode] = relations

  override def getRelations: List[Relation] = relations
}

case class Except(position: Option[SqlNodePosition] = None,
                  left: Relation,
                  right: Relation,
                  distinct: Option[Boolean] = None
                 ) extends SetOperation(position, distinct) {
  override def getChildren: List[SqlNode] = List(left, right)

  override def getRelations: List[Relation] = List(left, right)
}

case class Intersect(position: Option[SqlNodePosition] = None,
                     relations: List[Relation],
                     distinct: Option[Boolean] = None
                    ) extends SetOperation(position, distinct) {
  override def getChildren: List[SqlNode] = relations

  override def getRelations: List[Relation] = relations
}
