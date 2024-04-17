package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.SqlDialect
import io.octopus.sql.parser.token.WhiteSpace.{NEW_LINE, SPACE, TAB}
import io.octopus.sql.parser.token.{CharStream, EOF, Token, TokenStream, WhiteSpace}
import io.octopus.sql.utils.Engine.MYSQL

class SqlTokenizer(sqlDialect: SqlDialect) {
  def tokenize(sql:String):Either[SqlParsingException, TokenStream]={
    val chars = CharStream(sql)
    var token = nextToken(chars)
    ???
  }

  def nextToken(chars: CharStream):Either[SqlParsingException, Token] ={
    chars.peek() match{
      case Some(c) =>{
        c match{
          case ' ' => consumeAndReturn(chars,Token.whiteSpace(SPACE,chars.position))
          case '\t' => consumeAndReturn(chars,Token.whiteSpace(TAB,chars.position))
          case '\n' => consumeAndReturn(chars,Token.whiteSpace(NEW_LINE,chars.position))
          case '\r' => {
            chars.next()
            if(chars.peek().contains('\n')){
              chars.next()
            }
            Right(Token.whiteSpace(NEW_LINE,chars.position))
          }
          // hex String
          case 'X' =>
            chars.next()
            chars.peek() match
              // X'...' =>  <binary string literal>
              case Some('\'') => {
                val s = quotedString(chars,'\'')
                s match
                  case Right(s) => Right(Token.binaryLiteral(s,chars.position))
                  case Left(value) =>
              }
              case _ => ???
        }
      }
      case None => Token(EOF.toString,EOF,chars.position)
    }


    ???
  }

  def consumeAndReturn(chars: CharStream, t: Token):Either[SqlParsingException, Token] ={
    chars.next()
    Right(t)
  }

  def quotedString(chars: CharStream, quote:Char):Either[SqlParsingException, String] ={
    val start = chars.position
    val stringBuilder = new StringBuilder()
    // consume opening quote char
    chars.next()
    while(chars.peek().isDefined){
      val ch = chars.peek().get
      ch match
        case char if char == quote => {
          chars.next()
          if(chars.peek().contains(quote)){
            // escape mode
            stringBuilder.append(ch)
            chars.next()
          }else{
            // end of string
            return Right(stringBuilder.toString())
          }
        }
        case _ => {
          chars.next()
          stringBuilder.append(ch)
        }
    }
    Left(SqlParsingException(s"Unterminated string literal",start))
  }
}
