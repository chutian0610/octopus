package io.octopus.sql.parser.token

import io.octopus.sql.parser.Position
import io.octopus.sql.parser.token.WhiteSpace.{NEW_LINE, SPACE, TAB}

case class Token(text: String, tokenType: TokenType, position: Option[Position] = None) {
  override def toString: String = {
    val typeCatalog =  tokenType match {
      case literal: Literal=> "Literal"
      case word: Word => "Word"
      case symbol: Symbol => "Symbol"
      case whiteSpace: WhiteSpace => "WhiteSpace"
      case EOF => ""
    }
    position match {
      case Some(value) =>  s"Token(\"$text\", $typeCatalog.$tokenType, $position)"
      case None =>  s"Token(\"$text\", $typeCatalog.$tokenType)"
    }
  }
}

object Token{
  def apply(text: String, tokenType: TokenType, position: Position): Token = Token(text, tokenType, Some(position))

  def whiteSpace(whiteSpace: WhiteSpace, position: Position):Token = {
    whiteSpace match
      case SPACE => Token("\n",WhiteSpace.SPACE,position)
      case TAB => Token("\t",WhiteSpace.TAB,position)
      case NEW_LINE => Token("\n",WhiteSpace.NEW_LINE,position)
  }

  def binaryLiteral(text: String, position: Position):Token={
    Token(text,Literal.BINARY,position)
  }
}

sealed trait TokenType

enum Literal extends TokenType {
  case STRING
  , LONG
  , DOUBLE
  , DECIMAL
  , BINARY
}

enum Word extends TokenType {
  case IDENTIFIER
  , KEYWORD
}
object EOF extends TokenType{
  override def toString: String = "EOF"
}

enum WhiteSpace extends TokenType {
  case SPACE,NEW_LINE,TAB
}

enum Symbol extends TokenType {
  case EQ // `=`
  , NEQ // `<>` or `!=`
  , LT // `<`
  , LTE // `<=`
  , GT // `>`
  , GTE // `>=`
  , PLUS // `+`
  , MINUS // `-`
  , ASTERISK // `*`
  , DIV // `/`
  , PERCENT // `%`
  , CONCAT // `||`
  , LEFT_PAREN // `(`
  , RIGHT_PAREN // `)`
  , LEFT_BRACKET // `[`
  , RIGHT_BRACKET // `]`
  , LEFT_BRACE // `{`
  , RIGHT_BRACE // `}`
  , COMMA // `,`
  , DOT // `.`
  , SEMICOLON // `;`
  , COLON // `:`
  , DOUBLE_COLON // `::`
  , AT // `@`
  , QUESTION // `?`
  , ARROW // `->` or `=>`
}
