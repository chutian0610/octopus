package io.octopus.sql.parser.tree

import io.octopus.sql.parser.Position

import scala.collection.immutable.List

abstract class Relation(position: Option[Position] = None) extends SqlNode(position) {}


case class AliasedRelation(position: Option[Position] = None,
                           relation: Relation,
                           alias: Identifier,
                           columnAliases: List[Identifier]) extends Relation(position) {
  override def getChildren: List[SqlNode] = List(relation)
}

enum SampledType {
  case BERNOULLI, SYSTEM
}

case class SampledRelation(position: Option[Position] = None,
                           relation: Relation,
                           sampleType: SampledType,
                           samplePercentage: Expression
                          ) extends Relation(position) {
  override def getChildren: List[SqlNode] = List(relation, samplePercentage)
}

case class Unnest(position: Option[Position] = None,
                  expressions: List[Expression],
                  withOrdinality: Boolean = false) extends Relation(position) {
  override def getChildren: List[SqlNode] = expressions
}

case class Lateral(position: Option[Position] = None,
                   query: Query) extends Relation(position) {
  override def getChildren: List[SqlNode] = List(query)
}

abstract class QueryBody(position: Option[Position] = None) extends Relation(position) {}


case class QuerySpecification(position: Option[Position] = None,
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

case class SubQuery(position: Option[Position] = None,
                    query: Query
                   ) extends QueryBody(position) {
  override def getChildren: List[SqlNode] = List(query)
}

case class Table(position: Option[Position] = None,
                 name: QualifiedName
                ) extends QueryBody(position) {
  override def getChildren: List[SqlNode] = List.empty
}

case class Values(position: Option[Position] = None,
                  rows: List[Expression]
                 ) extends QueryBody(position) {
  override def getChildren: List[SqlNode] = rows
}

enum JoinType {
  case CROSS, INNER, LEFT, RIGHT, FULL, IMPLICIT
}

case class Join(position: Option[Position] = None,
                joinType: JoinType,
                left: Relation,
                right: Relation,
                criteria: Option[JoinCriteria] = None,
               ) extends Relation(position) {
  override def getChildren: List[SqlNode] = {
    left :: right :: criteria.toList
  }
}

abstract class JoinCriteria(position: Option[Position] = None) extends SqlNode(position)

case class JoinOn(position: Option[Position] = None, expression: Expression) extends JoinCriteria(position) {
  override def getChildren: List[SqlNode] = List(expression)
}

case class JoinUsing(position: Option[Position] = None, columns: List[Identifier]) extends JoinCriteria(position) {
  override def getChildren: List[SqlNode] = List.empty
}

case class NaturalJoin(position: Option[Position] = None) extends JoinCriteria(position) {
  override def getChildren: List[SqlNode] = List.empty
}

abstract class SetOperation(position: Option[Position] = None, distinct: Option[Boolean] = None) extends QueryBody(position) {
  def getRelations: List[Relation]
}

case class Union(position: Option[Position] = None,
                 relations: List[Relation],
                 distinct: Option[Boolean] = None
                ) extends SetOperation(position, distinct) {
  override def getChildren: List[SqlNode] = relations

  override def getRelations: List[Relation] = relations
}

case class Except(position: Option[Position] = None,
                  left: Relation,
                  right: Relation,
                  distinct: Option[Boolean] = None
                 ) extends SetOperation(position, distinct) {
  override def getChildren: List[SqlNode] = List(left, right)

  override def getRelations: List[Relation] = List(left, right)
}

case class Intersect(position: Option[Position] = None,
                     relations: List[Relation],
                     distinct: Option[Boolean] = None
                    ) extends SetOperation(position, distinct) {
  override def getChildren: List[SqlNode] = relations

  override def getRelations: List[Relation] = relations
}
