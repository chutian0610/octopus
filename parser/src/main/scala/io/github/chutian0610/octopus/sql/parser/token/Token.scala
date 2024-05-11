package io.github.chutian0610.octopus.sql.parser.token

import io.github.chutian0610.octopus.sql.parser.token.TokenType.{EOF, SINGLE_LINE_COMMENT}
import io.github.chutian0610.octopus.sql.parser.{Position, SqlParsingException}

import scala.util.Right

case class TokenWithPosition(token: Token, position: Option[Position]) extends Token{
  override def toString: String = {
    position match {
      case Some(value) => s"[$token, $position]"
      case None => s"[$token]"
    }
  }

  override def tokenType: TokenType = token.tokenType

  override def text: String = token.text
}

object TokenWithPosition:
  def apply(token: Token, position: Position): TokenWithPosition = {
    TokenWithPosition(token, Some(position))
  }
  def apply(token: Token): TokenWithPosition = {
    TokenWithPosition(token, None)
  }
end TokenWithPosition

sealed trait Token {
  def tokenType: TokenType
  def text: String

  /**
   * unwrap the token if token is instance of TokenWithPosition
   * @return
   */
  def unWrap: Token = this match{
    case tokenWithPosition: TokenWithPosition => tokenWithPosition.token
    case _ => this
  }

  /**
   * compare two token, return true if them are same
   * @param obj the other token
   * @return
   */
  def sameAs(obj: Token): Boolean = {
    if(obj == null){
      return false
    }
    // ignore TokenWithPosition
    val unWrapThis = this.unWrap
    val unWrapThat = obj.unWrap

    unWrapThis.equals(unWrapThat)
  }

  override def toString: String = {
    this match
      case singleLineComment: WhiteSpace.SINGLE_LINE_COMMENT =>
        return s"Token(text=\"$text\", type=$tokenType,prefix=${singleLineComment.prefix})"
      case multiLineComment: WhiteSpace.MULTI_LINE_COMMENT =>
        return s"Token(text=\"$text\", type=$tokenType,prefix=${multiLineComment.prefix},suffix=${multiLineComment.suffix})"
      case naturalString: Literal.NaturalString =>
        return s"Token(text=\"$text\", type=$tokenType,quote=${naturalString.quote})"
      case unicodeString: Literal.UnicodeString =>
        return s"Token(text=\"$text\", type=$tokenType,quote=${unicodeString.quote})"
      case number: Literal.Number =>
        return s"Token(text=\"$text\", type=$tokenType,isLong=${number.isLong})"
      case identifier: Word.Identifier if identifier.quote.isDefined=>
        return s"Token(text=\"$text\", type=$tokenType,quote=${identifier.quote.get})"
      case _ =>
    s"Token(text=\"$text\", type=$tokenType)"
  }
}

object Tokens {
  def arrow: Token = Symbol.ARROW()
  def space: Token = WhiteSpace.SPACE()
  def tab: Token = WhiteSpace.TAB()
  def newLine: Token = WhiteSpace.NEW_LINE()
  def eof: Token = EOF()
  def singleLineComment(text: String, prefix: String): Token = WhiteSpace.SINGLE_LINE_COMMENT(text, prefix)
  def multiLineComment(text: String, prefix: String, suffix: String): Token = WhiteSpace.MULTI_LINE_COMMENT(text, prefix, suffix)
  def leftParen: Token = Symbol.LEFT_PAREN()
  def rightParen: Token = Symbol.RIGHT_PAREN()
  def comma: Token = Symbol.COMMA()
  def minus: Token = Symbol.MINUS()
  def plus: Token = Symbol.PLUS()
  def asterisk:Token = Symbol.ASTERISK()
  def divide: Token = Symbol.DIVIDE()
  def percent: Token = Symbol.PERCENT()
  def hexString(text: String): Token = Literal.HexString(text)
  def naturalString(text: String, quote: Char): Token = Literal.NaturalString(text, quote)
  def unicodeString(text: String, quote: Char): Token = Literal.UnicodeString(text, quote)
  def number(text: String, isLong: Boolean): Token = Literal.Number(text, isLong)
  def identifier(text: String, quote: Option[Char]): Token = Word.Identifier(text, quote)
  def keyWord(k: KEYWORD): Token = Word.KeyWord(k)
  def dot: Token = Symbol.DOT()
  def concat: Token = Symbol.CONCAT()
  def eq:Token = Symbol.EQ()
  def neq:Token = Symbol.NEQ()
  def lte:Token = Symbol.LTE()
  def gte:Token = Symbol.GTE()
  def gt:Token = Symbol.GT()
  def lt:Token = Symbol.LT()
  def colon: Token = Symbol.COLON()
  def semiColon: Token = Symbol.SEMICOLON()
  def question: Token = Symbol.QUESTION()
}

case class EOF(text: String = "\u001A", tokenType: TokenType = TokenType.EOF) extends Token

enum Literal(text: String,
             tokenType: TokenType) extends Token {
  case HexString(text: String, tokenType: TokenType = TokenType.HEX_STRING) extends Literal(text, tokenType)
  case NaturalString(text: String, quote: Char, tokenType: TokenType = TokenType.NATURAL_String) extends Literal(text, tokenType)
  case UnicodeString(text: String, quote: Char, tokenType: TokenType = TokenType.UNICODE_STRING) extends Literal(text, tokenType)
  case Number(text: String, isLong: Boolean, tokenType: TokenType = TokenType.NUMBER) extends Literal(text, tokenType)
}

enum Word(content: String,
          tokenType: TokenType) extends Token {
  case KeyWord(k: KEYWORD, tokenType: TokenType = TokenType.KEYWORD) extends Word(k.entryName, tokenType)
  case Identifier(content: String, quote: Option[Char], tokenType: TokenType = TokenType.IDENTIFIER) extends Word(content, tokenType)

  override def text: String = {
    this match
      case Word.KeyWord(k, tokenType) => k.entryName
      case Word.Identifier(text, quote, tokenType) =>text
  }
}

object Word:
  def matchingEndQuote(c: Char): Either[SqlParsingException, Char] = {
    c match
      case '\"' => Right('\"')
      case '`' => Right('`')
      case '[' => Right(']')
      case _ => Left(SqlParsingException("unexpected quoting style!"))
  }
end Word

enum WhiteSpace(
                 text: String,

                 tokenType: TokenType) extends Token {
  case SPACE(text: String = " ", tokenType: TokenType = TokenType.SPACE) extends WhiteSpace(text, tokenType)
  case TAB(text: String = "\t", tokenType: TokenType = TokenType.TAB) extends WhiteSpace(text, tokenType)
  case NEW_LINE(text: String = "\n", tokenType: TokenType = TokenType.NEW_LINE) extends WhiteSpace(text, tokenType)
  case SINGLE_LINE_COMMENT(text: String, prefix: String, tokenType: TokenType = TokenType.SINGLE_LINE_COMMENT) extends WhiteSpace(text, tokenType)
  case MULTI_LINE_COMMENT(text: String, prefix: String, suffix: String, tokenType: TokenType = TokenType.SINGLE_LINE_COMMENT) extends WhiteSpace(text, tokenType)
}

enum Symbol(text: String,

            tokenType: TokenType) extends Token {
  case EQ(text: String = "=", tokenType: TokenType = TokenType.EQ) extends Symbol(text, tokenType)
  case NEQ(text: String = "<>", tokenType: TokenType = TokenType.NEQ) extends Symbol(text, tokenType)
  case LT(text: String = "<", tokenType: TokenType = TokenType.LT) extends Symbol(text, tokenType)
  case GT(text: String = ">", tokenType: TokenType = TokenType.GT) extends Symbol(text, tokenType)
  case LTE(text: String = "<=", tokenType: TokenType = TokenType.LTE) extends Symbol(text, tokenType)
  case GTE(text: String = ">=", tokenType: TokenType = TokenType.GTE) extends Symbol(text, tokenType)
  case DOT(text: String = ".", tokenType: TokenType = TokenType.DOT) extends Symbol(text, tokenType)
  case LEFT_PAREN(text: String = "(", tokenType: TokenType = TokenType.LEFT_PAREN) extends Symbol(text, tokenType)
  case RIGHT_PAREN(text: String = ")", tokenType: TokenType = TokenType.RIGHT_PAREN) extends Symbol(text, tokenType)
  case COMMA(text: String = ",", tokenType: TokenType = TokenType.COMMA) extends Symbol(text, tokenType)
  case MINUS(text: String = "-", tokenType: TokenType = TokenType.MINUS) extends Symbol(text, tokenType)
  case PLUS(text: String = "+", tokenType: TokenType = TokenType.PLUS) extends Symbol(text, tokenType)
  case ASTERISK(text: String = "*", tokenType: TokenType = TokenType.ASTERISK) extends Symbol(text, tokenType)
  case DIVIDE(text: String = "/", tokenType: TokenType = TokenType.DIVIDE) extends Symbol(text, tokenType)
  case PERCENT(text: String = "%", tokenType: TokenType = TokenType.PERCENT) extends Symbol(text, tokenType)
  case SEMICOLON(text: String = ";", tokenType: TokenType = TokenType.SEMICOLON) extends Symbol(text, tokenType)
  case ARROW(text: String = "->", tokenType: TokenType = TokenType.ARROW) extends Symbol(text, tokenType)
  case CONCAT(text: String = "||", tokenType: TokenType = TokenType.CONCAT) extends Symbol(text, tokenType)
  case COLON(text: String = ":", tokenType: TokenType = TokenType.COLON) extends Symbol(text, tokenType)
  case QUESTION(text: String = "?", tokenType: TokenType = TokenType.QUESTION) extends Symbol(text, tokenType)
}
