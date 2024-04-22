package io.octopus.sql.parser

import com.google.common.base.CharMatcher
import io.octopus.sql.parser.dialect.SqlDialect
import io.octopus.sql.parser.token.{CharStream, Token, TokenStream, TokenType, TokenWithPosition, Tokens, Word}
import io.octopus.sql.utils.Engine.MYSQL

class SqlTokenizer(sqlDialect: SqlDialect) {
  def tokenizeWithPosition(sql: String): Either[SqlParsingException, TokenStream[TokenWithPosition]] = {
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

  def tokenize(sql: String): Either[SqlParsingException, TokenStream[Token]] = {
    val chars = CharStream(sql)
    val tokens = List.newBuilder[Token]

    var token = nextToken(chars)
    while (token.isRight && !token.contains(Tokens.eof)) {
      tokens.addOne((token.toOption.get))
      token = nextToken(chars)
    }

    token match
      case Right(value) => Right(TokenStream(tokens.result()))
      case Left(exception) => Left(exception)
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
                scanQuotedString(chars, '\'').map(s=> Tokens.hexString(s))
              }
              //  identifier | KeyWord starting with an "X"
              case _ => {
                Right(buildWord(scanWord(x, chars),None))
              }
          }
          // single quoted string
          case '\'' => {
            scanQuotedString(chars, '\'').map(s => Tokens.naturalString(s,'\''))
          }
          // double quoted string
          case ch@('"') if !sqlDialect.startOfQuotedIdentifier(ch) && !sqlDialect.startOfIdentifier(ch) => {
            scanQuotedString(chars, '\'').map(s => Tokens.naturalString(s,'"'))
          }
          // quoted identifier
          case quote_start if sqlDialect.startOfDelimitedIdentifier(quote_start) => {
            chars.next // consume the opening quote
            val quote_end = Word.matchingEndQuote(quote_start)
            quote_end match
              case Right(end) => {
                scanQuotedIdentifier(chars, end) match
                  case Right((s,end)) if end.contains(quote_end) => {
                    Right(buildWord(s, end))
                  }
                  case Right((s, end)) => {
                    Left(SqlParsingException(s"Expected close delimiter '${quote_end}' before EOF."))
                  }
                  case Left(exception) => Left(exception)
                }
              case Left(exception) => Left(exception)
          }
          // number
          case n if ('0' to '9' contains n) || n == '.' => {
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
              chars_clone.peek match {
                case Some(x) if x == '+' || x == '-' => {
                  exponent_part.append(x)
                  chars_clone.next
                }
                case _ =>
              }
              chars_clone.peek match
                case Some(x) if CharMatcher.digit().matches(x) => {
                    // skip exponent
                    chars.next(exponent_part.size)
                    exponent_part.append(chars.peekCharsWhile(x => CharMatcher.digit().matches(x)))
                    sb.append(exponent_part)
                }
                case _ =>
            }
            // mysql dialect supports identifiers that start with a numeric prefix,
            // as long as they aren't an exponent number.
            if(sqlDialect.dialectOf(MYSQL) && exponent_part.isEmpty){
              val word = chars.peekCharsWhile(x=>sqlDialect.partOfIdentifier(x))
              if(word.nonEmpty){
                sb.append(word)
              }
              return Right(buildWord(sb.toString(), None))
            }
            val isLong = if(chars.peek.contains('L')){
              chars.next
              true
            }else false
            Right(Tokens.number(sb.toString(), isLong))
          }
          // punctuation
          case '(' => consumeAndReturn(chars, Tokens.leftParen)
          case ')' => consumeAndReturn(chars, Tokens.rightParen)
          case ',' => consumeAndReturn(chars, Tokens.comma)
          case '-' =>{
            chars.next // consume '-'
            chars.peek match {
              case Some(c) if c == '-' => {
                //  single-line comment
                chars.next // consume '-'
                // single line comment starts with `--`
                scanSingleLineComment(chars).map(comment => Tokens.singleLineComment(comment,"--"))
              }
              case Some(c) if c == '>' => {
                chars.next // consume '>'
                Right(Tokens.arrow) // Symbol `->`
              }
              case _ => Right(Tokens.minus)
            }
          }
          case '/' => {
            chars.next // consume '/'
            chars.peek match {
              case Some(c) if c == '*' => {
                //  multi-line comment
                chars.next // consume '*'
                scanMultiLineComment(chars, "/*", "*/").map(comment => Tokens.multiLineComment(comment, "/*", "*/"))
              }
              case _ => Right(Tokens.divide)
            }
          }
          case '+' => consumeAndReturn(chars, Tokens.plus)
          case '*' => consumeAndReturn(chars, Tokens.asterisk)
          case '%' => consumeAndReturn(chars, Tokens.percent)
          case '|' => {
            chars.next // consume '|'
            chars.peek match {
              case Some(c) if c == '|' => {
                //  String concat
                chars.next // consume '|'
                Right(Tokens.concat)
              }
              case oc => {
                val result = ""+'|'+oc.getOrElse("")
                Left(SqlParsingException(s"Expected String Concat but found $result"))
              }
            }
          }
          case '=' => consumeAndReturn(chars, Tokens.eq)
          case '!' =>{
            chars.next // consume '!'
            chars.peek match {
              case Some(c) if c == '=' => {
                chars.next // consume '='
                Right(Tokens.neq)
              }
              case oc => {
                val result = "" + '!' + oc.getOrElse("")
                Left(SqlParsingException(s"Expected '!=' but found ${result}"))
              }
            }
          }
          case '<' => {
            chars.next // consume '<'
            chars.peek match {
              case Some(c) if c == '=' => consumeAndReturn(chars, Tokens.lte)
              case Some(c) if c == '>' => consumeAndReturn(chars, Tokens.neq)
              case _ => Right(Tokens.lt)
            }
          }
          case '>' => {
            chars.next // consume '>'
            chars.peek match
              case Some(c) if c == '=' => consumeAndReturn(chars, Tokens.gte)
              case _ => Right(Tokens.gt)
          }
          case ':' => consumeAndReturn(chars,Tokens.colon)
          case ';' => consumeAndReturn(chars, Tokens.semiColon)
          case '?' => consumeAndReturn(chars,Tokens.question)
          case ch if sqlDialect.startOfIdentifier(ch) => {
            chars.next
            Right(buildWord(scanWord(ch, chars), None))
          }
        }
      }
      case None => Right(Tokens.eof)
    }
  }

  private def consumeAndReturn(chars: CharStream, t: Token): Either[SqlParsingException, Token] = {
    chars.next
    Right(t)
  }

  def buildWord(text: String, quote: Option[Char]): Token = {
    sqlDialect.matchKeyWord(text) match
      case Some(keyword) => {
        Tokens.keyWord(text)
      }
      case None => {
        Tokens.identifier(text, quote)
      }
  }

  private def scanSingleLineComment(chars: CharStream): Either[SqlParsingException, String] = {
    val sb = new StringBuilder()
    val comment = chars.peekCharsWhile(x => x != '\n')
    Right(comment)
  }
  private def scanMultiLineComment(chars: CharStream, prefix: String ,suffix :String): Either[SqlParsingException, String] = {
    val sb = new StringBuilder()
    var ch: Option[Char] = chars.next
    var last_ch: Char = ' '
    var nested = 1
    while (ch.isDefined) {
      if ( ("" + last_ch+ ch.get ) == prefix) {
        nested +=1
      }
      if ((""+last_ch + ch.get) == suffix){
        nested -= 1;
        if(nested ==0) return Right(sb.toString())
      }
      sb.append(ch)
      last_ch = ch.get
      ch = chars.next
    }
    // incomplete Token
    Left(SqlParsingException( s"Unexpected EOF while in a multi-line comment:${sb.toString()}"))
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
    Left(SqlParsingException(s"Unterminated quoted Identifier:${sb.toString()}"))
  }

  private def scanWord(first: Char, chars: CharStream): String = {
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
    Left(SqlParsingException(s"Unterminated string literal:${sb.toString()}"))
  }
}
