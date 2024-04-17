package io.octopus.sql.parser

import com.google.common.base.CharMatcher
import io.octopus.sql.parser.dialect.SqlDialect
import io.octopus.sql.parser.token.{CharStream, Token, TokenStream, TokenType, TokenWithPosition, Tokens, Word}
import io.octopus.sql.utils.Engine.MYSQL

class SqlTokenizer(sqlDialect: SqlDialect) {
  def tokenize(sql: String): Either[SqlParsingException, TokenStream] = {
    val chars = CharStream(sql)
    val tokens = List.newBuilder[TokenWithPosition]

    var position = chars.position
    var token = nextToken(chars)
    while (token.isRight && !token.contains(Tokens.eof)) {
      tokens.addOne(TokenWithPosition(token.toOption.get, position))
      position = chars.position
      token = nextToken(chars)
    }

    token match
      case Left(exception) => Left(SqlParsingException(exception, position))
      case Right(value) => Right(TokenStream(tokens.result()))
  }

  def nextToken(chars: CharStream): Either[SqlParsingException, Token] = {
    chars.peek match {
      case Some(c) => {
        c match {
          case ' ' => consumeAndReturn(chars, Tokens.space)
          case '\t' => consumeAndReturn(chars, Tokens.tab)
          case '\n' => consumeAndReturn(chars, Tokens.newLine)
          case '\r' => {
            chars.next
            if (chars.peek.contains('\n')) {
              chars.next
            }
            Right(Tokens.newLine)
          }
          // hex String
          case x@('X' | 'x') => {
            chars.next
            chars.peek match
              // X'...' =>  <binary string literal>
              case Some('\'') => {
                val s = scanQuotedString(chars, '\'')
                s match
                  case Right(s) => Right(Tokens.hexString(s))
                  case Left(value) =>
              }
              // keyword start with 'x'
              case _ => {
                val s = scanKeyWord(x, chars)
                Right(Tokens.keyWord(s))
              }
          }
          // single quoted string
          case '\'' => {
            val s = scanQuotedString(chars, '\'')
            s match
              case Right(s) => Right(Tokens.singleQuotedString(s))
              case Left(value) =>
          }
          // double quoted string
          case ch@('"') if !sqlDialect.startOfQuotedIdentifier(ch) && !sqlDialect.startOfIdentifier(ch) => {
            val s = scanQuotedString(chars, '\'')
            s match
              case Right(s) => Right(Tokens.doubleQuotedString(s))
              case Left(value) =>
          }
          // quoted identifier
          case quote_start if sqlDialect.startOfIdentifier(quote_start) => {
            chars.next // consume the opening quote
            Word.matchingEndQuote(quote_start) match
              case Right(quote_end) => {
                scanQuotedIdentifier(chars, quote_end) match
                  case Right((s, end)) => {
                    if (end.contains(quote_end)) {
                      Right(Tokens.identifier(s, end))
                    }
                  }
                  case Left(value) =>
              }
              case Left(value) =>
          }
          // number
          case n if (0 to 9 contains n) || n == '.' => {
            val sb = new StringBuilder()
            val s = chars.peekCharsWhile(x => CharMatcher.digit().matches(x))
            sb.append(s)
            // match binary literal that starts with 0x
            if (s == "0" && chars.peek.contains('x')) {
              chars.next // consumer 'x'
              val s2 = chars.peekCharsWhile(x => CharMatcher.digit()
                .and(CharMatcher.inRange('a', 'z'))
                .and(CharMatcher.inRange('A', 'Z')).matches(x));
              return Right(Tokens.hexString(s2))
            }
            // match decimal point
            if(chars.peek.contains('.')){
              sb.append(".")
              chars.next
            }
            sb.append(chars.peekCharsWhile(x => CharMatcher.digit().matches(x)))
            // maybe is '.'
            if(sb.toString() ==  "."){
              return Right(Tokens.dot)
            }
            // parse exponent
            val exponent_part = new StringBuilder()
            if(chars.peek.exists(x => x == 'e' || x == 'E')){
              val chars_clone = chars.copy()
              exponent_part.append(chars_clone.next)
            }

          }
        }
      }
      case None => Tokens.eof
    }


    ???
  }

  def consumeAndReturn(chars: CharStream, t: Token): Either[SqlParsingException, Token] = {
    chars.next
    Right(t)
  }

  private def scanQuotedIdentifier(chars: CharStream, quoteEnd: Char): Either[SqlParsingException, (String, Option[Char])] = {
    var lastChar: Option[Char] = None
    val sb = new StringBuilder()
    var ch = chars.next
    while (ch.isDefined) {
      if (ch.get == quoteEnd) {
        chars.peek match {
          case Some(c) if c == quoteEnd => {
            // escape mode
            chars.next // consume escaped char
            sb.append(ch)
          }
          case _ => {
            lastChar = Some(quoteEnd)
            return Right(sb.toString(), lastChar)
          }
        }
      }
      else {
        sb.append(ch)
      }
      ch = chars.next
    }
    // incomplete Token
    Left(SqlParsingException(s"Unterminated quoted Identifier"))
  }

  private def scanKeyWord(first: Char, chars: CharStream): String = {
    val sb = new StringBuilder()
    sb.append(first)
    sb.append(chars.peekCharsWhile(x => sqlDialect.partOfIdentifier(x)))
    sb.toString()
  }

  private def scanQuotedString(chars: CharStream, quote: Char): Either[SqlParsingException, String] = {
    val sb = new StringBuilder()
    // consume opening quote char
    chars.next
    while (chars.peek.isDefined) {
      val ch = chars.peek.get
      ch match
        case char if char == quote => {
          chars.next
          if (chars.peek.contains(quote)) {
            // escape mode
            sb.append(ch)
            chars.next
          } else {
            // end of string
            return Right(sb.toString())
          }
        }
        case _ => {
          chars.next
          sb.append(ch)
        }
    }
    // incomplete Token
    Left(SqlParsingException(s"Unterminated string literal"))
  }
}
