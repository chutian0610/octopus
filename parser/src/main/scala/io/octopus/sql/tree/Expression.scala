package io.octopus.sql.tree

import io.octopus.sql.parser.SqlNodePosition
import io.octopus.sql.utils.Engine

abstract class Expression(position: Option[SqlNodePosition] = None) extends SqlNode(position) {}

case class Identifier(position: Option[SqlNodePosition] = None,
                      value: String,
                      delimited: Boolean = false
                     ) extends Expression(position) {
  override def getChildren: List[SqlNode] = List.empty
}


case class QualifiedName(position: Option[SqlNodePosition] = None,
                         parts: List[Identifier]
                        ) extends Expression(position) {
  override def getChildren: List[SqlNode] = List.empty
}

object QualifiedName {
  def apply(value: String): QualifiedName = {
    QualifiedName(parts = List(Identifier(value = value)))
  }

  def apply(value: List[String]): QualifiedName = {
    QualifiedName(parts = value.map(value => Identifier(value = value)))
  }
}
case class DereferenceExpression(position: Option[SqlNodePosition] = None,
                                 base: Expression,
                                 field: Identifier) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(base)
}

// ********************************** BooleanExpression **********************************

enum LogicalOperator {
  case AND, OR

  def flip: LogicalOperator = this match {
    case AND =>
      OR
    case OR =>
      AND
  }
}

case class LogicalNotExpression(position: Option[SqlNodePosition] = None, expression: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(expression)
}

case class LogicalBinaryExpression(position: Option[SqlNodePosition] = None, operator: LogicalOperator, left: Expression, right: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(left, right)
}

object LogicalBinaryExpression {
  def and(left: Expression, right: Expression): LogicalBinaryExpression = LogicalBinaryExpression(Option.empty, LogicalOperator.AND, left, right)

  def or(left: Expression, right: Expression): LogicalBinaryExpression = LogicalBinaryExpression(Option.empty, LogicalOperator.OR, left, right)
}


// ********************************** predicates **********************************
enum ComparisonOperator {
  case EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IS_DISTINCT_FROM

  def negate: ComparisonOperator = this match {
    case EQUAL =>
      NOT_EQUAL
    case NOT_EQUAL =>
      EQUAL
    case GREATER_THAN =>
      LESS_THAN_OR_EQUAL
    case GREATER_THAN_OR_EQUAL =>
      LESS_THAN
    case LESS_THAN =>
      GREATER_THAN_OR_EQUAL
    case LESS_THAN_OR_EQUAL =>
      GREATER_THAN
    case _ =>
      throw new IllegalArgumentException("Unsupported comparison: " + this);
  }

  def flip: ComparisonOperator = this match {
    case EQUAL =>
      EQUAL
    case NOT_EQUAL =>
      NOT_EQUAL
    case GREATER_THAN =>
      LESS_THAN
    case GREATER_THAN_OR_EQUAL =>
      LESS_THAN_OR_EQUAL
    case LESS_THAN =>
      GREATER_THAN
    case LESS_THAN_OR_EQUAL =>
      GREATER_THAN_OR_EQUAL
    case IS_DISTINCT_FROM =>
      IS_DISTINCT_FROM
  }
}

case class ComparisonExpression(position: Option[SqlNodePosition] = None,
                                operator: ComparisonOperator,
                                left: Expression,
                                right: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(left, right)
}

case class NotExpression(position: Option[SqlNodePosition] = None, expression: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(expression)
}

case class BetweenPredicate(position: Option[SqlNodePosition] = None, value: Expression, min: Expression, max: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(value, min, max)
}

case class IsNullPredicate(position: Option[SqlNodePosition] = None, value: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(value)
}

case class IsNotNullPredicate(position: Option[SqlNodePosition] = None,
                              value: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(value)
}

case class LikePredicate(position: Option[SqlNodePosition] = None,
                         value: Expression,
                         pattern: Expression,
                         escape: Option[Expression] = None) extends Expression(position) {
  override def getChildren: List[SqlNode] = {
    value :: pattern :: escape.toList
  }
}

case class InPredicate(position: Option[SqlNodePosition] = None,
                       value: Expression,
                       valueList: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(value, valueList)
}

case class InListExpression(position: Option[SqlNodePosition] = None,
                            values: List[Expression]) extends Expression(position) {
  override def getChildren: List[SqlNode] = values
}

case class SubqueryExpression(position: Option[SqlNodePosition] = None,
                              query: Query) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(query)
}

case class ExistsPredicate(position: Option[SqlNodePosition] = None,
                           subExpression: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(subExpression)
}

enum QuantifierType {
  case ALL, ANY, SOME
}

case class QuantifiedComparisonExpression(position: Option[SqlNodePosition] = None,
                                          comparisonOperator: ComparisonOperator,
                                          quantifierType: QuantifierType,
                                          value: Expression,
                                          subquery: Expression
                                         ) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(value, subquery)
}

// ********************************** value expressions **********************************

enum ArithmeticUnarySign {
  case PLUS, MINUS
}

case class ArithmeticUnaryExpression(position: Option[SqlNodePosition] = None,
                                     sign: ArithmeticUnarySign,
                                     value: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(value)
}

enum ArithmeticBinaryOperator {
  case ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULUS
}

case class ArithmeticBinaryExpression(position: Option[SqlNodePosition] = None,
                                      operator: ArithmeticBinaryOperator,
                                      left: Expression,
                                      right: Expression) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(left, right)
}


// ********************************** FunctionCall **********************************
case class FunctionCall(position: Option[SqlNodePosition] = None,
                        name: QualifiedName,
                        window: Option[Window] = None,
                        filter: Option[Expression] = None,
                        orderBy: Option[OrderBy] = None,
                        distinct: Boolean = false,
                        ignoreNulls: Boolean = false,
                        arguments: List[Expression],
                        engine: Engine
                       ) extends Expression(position) {
  override def getChildren: List[SqlNode] = {
    val builder = List.newBuilder[SqlNode]
    if (window.isDefined) builder.addOne(window.get)
    if (filter.isDefined) builder.addOne(filter.get)
    if (orderBy.isDefined) orderBy.get.sortItems.foreach(x => builder.addOne(x))
    builder.addAll(arguments)
    builder.result()
  }
}

case class Window(position: Option[SqlNodePosition] = None,
                  partitionBy: List[Expression],
                  orderBy: Option[OrderBy] = None,
                  frame: Option[WindowFrame] = None
                 ) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = {
    val builder = List.newBuilder[SqlNode].addAll(partitionBy)
    if (orderBy.isDefined) orderBy.get.sortItems.foreach(x => builder.addOne(x))
    if (frame.isDefined) builder.addOne(frame.get)
    builder.result()
  }
}

enum WindowFrameType {
  case ROWS, RANGE, GROUPS
}

case class WindowFrame(position: Option[SqlNodePosition] = None,
                       frameType: WindowFrameType,
                       start: WindowFrameBound,
                       end: Option[WindowFrameBound] = None
                      ) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = start :: end.toList
}

enum WindowFrameBoundType {
  case UNBOUNDED_PRECEDING, PRECEDING, CURRENT_ROW, FOLLOWING, UNBOUNDED_FOLLOWING
}

case class WindowFrameBound(position: Option[SqlNodePosition] = None,
                            BoundType: WindowFrameBoundType,
                            value: Option[Expression]) extends SqlNode(position) {
  override def getChildren: List[SqlNode] = value.toList
}

case class LambdaExpression(position: Option[SqlNodePosition] = None,
                            arguments: List[LambdaArgumentDeclaration],
                            body: Expression
                           ) extends Expression(position) {
  override def getChildren: List[SqlNode] = {
    val builder = List.newBuilder[SqlNode].addAll(arguments)
    builder.addOne(body)
    builder.result()
  }
}

case class LambdaArgumentDeclaration(position: Option[SqlNodePosition] = None,
                                     name: Identifier) extends Expression {
  override def getChildren: List[SqlNode] = List.empty
}

case class AtTimeZone(position: Option[SqlNodePosition] = None,
                      value: Expression,
                      timeZone: Expression
                     ) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(value, timeZone)
}

case class RowConstructor(position: Option[SqlNodePosition] = None, items: List[Expression]) extends Expression(position) {
  override def getChildren: List[SqlNode] = items
}

case class ArrayConstructor(position: Option[SqlNodePosition] = None, values: List[Expression]) extends Expression(position) {
  override def getChildren: List[SqlNode] = values
}

case class Cast(position: Option[SqlNodePosition] = None,
                expression: Expression,
                toType: String,
                safe: Boolean = false,
                typeOnly: Boolean = false
               ) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(expression)
}

case class Subscript(position: Option[SqlNodePosition] = None,
                     base: Expression,
                     index: Expression
                    ) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(base, index)
}

case class SimpleCaseExpression(position: Option[SqlNodePosition] = None,
                                operand: Expression,
                                whenClauses: List[WhenClause],
                                defaultValue: Option[Expression] = None
                               ) extends Expression(position) {
  override def getChildren: List[SqlNode] = {
    val builder = List.newBuilder[SqlNode]
      .addOne(operand)
      .addAll(whenClauses)
    if (defaultValue.isDefined) builder.addOne(defaultValue.get)
    builder.result()
  }
}

case class SearchedCaseExpression(position: Option[SqlNodePosition] = None,
                                  whenClauses: List[WhenClause],
                                  defaultValue: Option[Expression] = None
                                 ) extends Expression(position) {
  override def getChildren: List[SqlNode] = {
    val builder = List.newBuilder[SqlNode]
      .addAll(whenClauses)
    if (defaultValue.isDefined) builder.addOne(defaultValue.get)
    builder.result()
  }
}

case class WhenClause(position: Option[SqlNodePosition] = None,
                      operand: Expression,
                      result: Expression
                     ) extends Expression(position) {
  override def getChildren: List[SqlNode] = List(operand, result)
}

case class GroupingOperation(position: Option[SqlNodePosition] = None, groupingColumns: List[QualifiedName]) extends Expression {
  override def getChildren: List[SqlNode] = List()
}

