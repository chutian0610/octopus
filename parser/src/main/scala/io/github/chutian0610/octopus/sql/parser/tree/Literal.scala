package io.github.chutian0610.octopus.sql.parser.tree

import com.google.common.io.BaseEncoding
import io.github.chutian0610.octopus.sql.parser.{Position, SqlParsingException}

import java.util.Locale
import java.util.regex.Pattern
sealed abstract class Literal(position: Option[Position] = None) extends Expression(position) {
  override def getChildren: List[SqlNode] = List.empty
}
case class NullLiteral(position: Option[Position] = None) extends Literal(position) {}

case class BooleanLiteral(position: Option[Position] = None, value: Boolean) extends Literal(position) {}

object BooleanLiteral {
  val TRUE_LITERAL: BooleanLiteral = fromString(Option.empty, "true")
  val FALSE_LITERAL: BooleanLiteral = fromString(Option.empty, "false")
  def fromString(position: Option[Position] = None, value: String): BooleanLiteral = {

    require(value != null)
    require(value.toLowerCase(Locale.ENGLISH).equals("true") || value.toLowerCase(Locale.ENGLISH).equals("false"))
    BooleanLiteral(position, value.toLowerCase(Locale.ENGLISH).equals("true"))
  }
}

case class BinaryLiteral(position: Option[Position] = None, value: Array[Byte]) extends Literal(position) {
  def toHexString: String = BaseEncoding.base16().encode(value)
}

object BinaryLiteral{

  private val WHITESPACE_PATTERN: Pattern =  Pattern.compile("[ \\r\\n\\t]")
  private val NOT_HEX_DIGIT_PATTERN:Pattern =  Pattern.compile(".*[^A-F0-9].*")
  def fromString(position: Option[Position] = None, value: String): BinaryLiteral = {
    require(value != null)
    val hexString = WHITESPACE_PATTERN.matcher(value).replaceAll("").toUpperCase()
    if (NOT_HEX_DIGIT_PATTERN.matcher(hexString).matches()) {
      throw SqlParsingException("Binary literal can only contain hexadecimal digits",position)
    }
    if (hexString.length() % 2 != 0) {
      throw SqlParsingException("Binary literal must contain an even number of digits", position)
    }
    BinaryLiteral(position, BaseEncoding.base16().decode(hexString))
  }
}

case class StringLiteral(position: Option[Position] = None, value: String) extends Literal(position)
case class IntegerLiteral(position: Option[Position] = None, value: Int) extends Literal(position)
case class LongLiteral(position: Option[Position] = None, value: Long) extends Literal(position)
object IntegerLiteral {
  def apply(position: Option[Position], value: String): IntegerLiteral = {
    IntegerLiteral(position, value.toInt)
  }

  def apply(value: String): IntegerLiteral = {
    IntegerLiteral(None, value.toInt)
  }
}
case class DoubleLiteral(position: Option[Position] = None, value: Double) extends Literal(position)
object DoubleLiteral {
  def apply(position: Option[Position], value: String): DoubleLiteral = {
    DoubleLiteral(position, value.toDouble)
  }

  def apply(value: String): DoubleLiteral = {
    DoubleLiteral(None, value.toDouble)
  }
}
case class DecimalLiteral(position: Option[Position] = None, value: String) extends Literal(position)


enum IntervalSign {
  case POSITIVE, NEGATIVE
}

enum IntervalField {
  case YEAR, MONTH, DAY, HOUR, MINUTE, SECOND
}

case class IntervalLiteral(position: Option[Position] = None,
                           value: String,
                           sign: IntervalSign,
                           startField: IntervalField,
                           endField: Option[IntervalField] = None
                          ) extends Literal(position)

case class GenericLiteral(position: Option[Position] = None,
                          literalType: String,
                          value: String) extends Literal(position)

case class DateLiteral(position: Option[Position] = None, value: String) extends Literal(position)
case class TimeLiteral(position: Option[Position] = None, value: String) extends Literal(position)
case class TimestampLiteral(position: Option[Position] = None, value: String) extends Literal(position)

