package io.octopus.sql.parser.token

import io.octopus.sql.parser.token.TokenType.EOF
import io.octopus.sql.parser.{Position, SqlParsingException}

import scala.util.Right

case class TokenWithPosition(token: Token, position: Option[Position]) {
  override def toString: String = {
    position match {
      case Some(value) => s"Token($token, $position)"
      case None => s"Token($token)"
    }
  }
}

object TokenWithPosition:
  def apply(token: Token, position: Position): TokenWithPosition = {
    TokenWithPosition(token, Some(position))
  }
end TokenWithPosition

sealed trait Token {
  def tokenType: TokenType

  def text: String

  override def toString: String = {
    val typeCatalog = this match {
      case literal: Literal => "Literal"
      case word: Word => "Word"
      case symbol: Symbol => "Symbol"
      case whiteSpace: WhiteSpace => "WhiteSpace"
    }
    s"\"$text\", $typeCatalog.$tokenType"
  }
}

object Tokens {
  def space: Token = {
    WhiteSpace.SPACE(" ")
  }

  def tab: Token = {
    WhiteSpace.TAB("\t")
  }

  def newLine: Token = {
    WhiteSpace.NEW_LINE("\n")
  }

  def eof: Token = {
    WhiteSpace.EOF("\u001A")
  }

  def hexString(text: String): Token = {
    Literal.HexString(text)
  }

  def singleQuotedString(text: String): Token = {
    Literal.SingleQuotedString(text)
  }

  def doubleQuotedString(text: String): Token = {
    Literal.DoubleQuotedString(text)
  }

  def identifier(text: String, quote: Option[Char]): Token = {
    Word.Identifier(text, quote)
  }

  def keyWord(text: String): Token = {
    Word.KeyWord(text)
  }
}


enum Literal(text: String,
             tokenType: TokenType) extends Token {
  case HexString(text: String, tokenType: TokenType = TokenType.HEX_STRING) extends Literal(text, tokenType)
  case SingleQuotedString(text: String, tokenType: TokenType = TokenType.SINGLE_QUOTED_STRING) extends Literal(text, tokenType)
  case DoubleQuotedString(text: String, tokenType: TokenType = TokenType.DOUBLE_QUOTED_STRING) extends Literal(text, tokenType)
}

enum Word(text: String,

          tokenType: TokenType) extends Token {
  case KeyWord(text: String, tokenType: TokenType = TokenType.KEYWORD) extends Word(text, tokenType)
  case Identifier(text: String, quote: Option[Char], tokenType: TokenType = TokenType.IDENTIFIER) extends Word(text, tokenType)
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
  case SPACE(text: String, tokenType: TokenType = TokenType.SPACE) extends WhiteSpace(text, tokenType)
  case TAB(text: String, tokenType: TokenType = TokenType.TAB) extends WhiteSpace(text, tokenType)
  case NEW_LINE(text: String, tokenType: TokenType = TokenType.NEW_LINE) extends WhiteSpace(text, tokenType)
  case EOF(text: String, tokenType: TokenType = TokenType.EOF) extends WhiteSpace(text, tokenType)
}

enum Symbol(text: String,

            tokenType: TokenType) extends Token {
  case EQ(text: String, tokenType: TokenType = TokenType.EQ) extends Symbol(text, tokenType)
}
