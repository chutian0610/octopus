package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.SqlDialect
import io.octopus.sql.parser.token.*
import io.octopus.sql.parser.tree.{Identifier, Query, SqlNode, WithClause, WithQuery}

import scala.util.boundary
import scala.util.boundary.break

/**
 * SqlParser
 */
class SqlParser(sqlDialect: SqlDialect, sqlParingOption: SqlParingOption = SqlParingOption()) {
  val tokenizer: SqlTokenizer = new SqlTokenizer(sqlDialect, sqlParingOption)

  def parseSql(sql: String): Either[SqlParsingException, List[SqlNode]] = {
    // sql -> token stream
    val tokenized = tokenizer.tokenizeWithPosition(sql)
    tokenized match {
      case Left(e) => Left(e)
      case Right(tokens) => parseStatements(tokens) // token stream -> AST
    }
  }
  // ============================== parser functions ===============================

  /**
   * Parse the token stream into statements
   *
   * @param tokenStream The token stream to parse
   * @return
   */
  def parseStatements(tokenStream: TokenStream): Either[SqlParsingException, List[SqlNode]] = {
    var expecting_statement_delimiter: Boolean = false
    val list = List.newBuilder[SqlNode]
    while (!tokenStream.peekAndSkipWhitespace.exists(_.unWrap.equals(Tokens.eof))) {
      // ignore empty statements (between successive statement delimiters)
      while (consumeToken(tokenStream, Tokens.semiColon)) {
        expecting_statement_delimiter = false
      }
      if (expecting_statement_delimiter) {
        return Left(expected("end of statement[;]", tokenStream.peek.get))
      }
      val statement = parseStatement(tokenStream)
      statement match
        case Left(e) => return Left(e)
        case Right(s) => list.addOne(s)
      expecting_statement_delimiter = true
    }
    Right(list.result())
  }

  /**
   * parse a single statement
   *
   * @param tokenStream The token stream to parse
   * @return
   */
  def parseStatement(tokenStream: TokenStream): Either[SqlParsingException, SqlNode] = {
    val token = tokenStream.peekAndSkipWhitespace.map(_.unWrap)
    token match
      case Some(w: Word.KeyWord) => {
        w.k match {
          case value@(KEYWORDS.SELECT | KEYWORDS.WITH | KEYWORDS.VALUES) => {
            // Query Statement
            parseQuery(tokenStream)
          }
          case _ => expected("an SQL statement", tokenStream.peek.get)
        }
      }
      case _ =>
    ???
  }

  def parseQuery(tokenStream: TokenStream): Either[SqlParsingException, Query] = {
    // parse with clause
    val withClause: Option[WithClause] = if (expectKeyWord(tokenStream, KEYWORDS.WITH)) {
      val kw_with = tokenStream.nextAndSkipWhitespace.get
      val position = kw_with.position
      val recursive = parseKeyWord(tokenStream, KEYWORDS.RECURSIVE)
      parseCommaSeparatedList[WithQuery](tokenStream, parseNamedQuery) match
        case Left(e) => return Left(e)
        case Right(namedQueries) => Some(WithClause(
          position, recursive, namedQueries
        ))
    } else {
      None
    }
    //

    ???
  }

  def parseCommaSeparatedList[T](tokenStream: TokenStream, itemParser: TokenStream => Either[SqlParsingException, T]): Either[SqlParsingException, List[T]] = {
    val list = List.newBuilder[T]
    boundary {
      while (true) {
        val item = itemParser(tokenStream)
        item match
          case Left(e) => return Left(e)
          case Right(i) => list.addOne(i)
        if (!consumeToken(tokenStream, Tokens.comma)) {
          break()
        }
      }
    }
    Right(list.result())
  }

  //  (`name [( col1, col2, ... )] AS (subquery)`)
  def parseNamedQuery(tokenStream: TokenStream): Either[SqlParsingException, WithQuery] = {
    parseIdentifier(tokenStream).flatMap(name => {
      if (parseKeyWord(tokenStream, KEYWORDS.AS)) {
        // parse subquery
        expectToken(tokenStream, Tokens.leftParen).flatMap(_ => {
          parseQuery(tokenStream).flatMap(subquery => {
            expectToken(tokenStream, Tokens.rightParen).flatMap(_ => {
              Right(WithQuery(
                position = name.position,
                name = name,
                columnNames = None,
                query = subquery))
            })
          })
        })
      } else {
        // parse column names

        ???
      }
    })
  }

  /**
   * parse identifier. (possibly quoted)
   *
   * @param tokenStream The token stream to parse
   * @return
   */
  def parseIdentifier(tokenStream: TokenStream): Either[SqlParsingException, Identifier] = {
    tokenStream.nextAndSkipWhitespace match
      case Some(w: Word.Identifier, p: Option[Position]) => Right(Identifier(
        p,
        w.content,
        w.quote.isDefined
      ))
      case Some(w) => Left(expected("identifier", w))
      case None => Left(expectedButNone("identifier"))
  }

  def expectKeyWord(tokenStream: TokenStream, expected: KEYWORD): Boolean = {
    tokenStream.peekAndSkipWhitespace.map(_.unWrap) match {
      case Some(kw: Word.KeyWord) if kw.k == expected => true
      case _ => false
    }
  }

  def parseKeyWord(tokenStream: TokenStream, expected: KEYWORD): Boolean = {
    tokenStream.peekAndSkipWhitespace.map(_.unWrap) match
      case Some(w: Word.KeyWord) if w.k == expected => {
        // consume token
        tokenStream.nextAndSkipWhitespace
        true
      }
      case _ => false
  }

  def consumeToken(tokenStream: TokenStream, expect: Token): Boolean = {
    val peek = tokenStream.peekAndSkipWhitespace
    if (peek.isDefined && peek.exists(t => t.sameAs(expect))) {
      tokenStream.nextAndSkipWhitespace
      true
    } else {
      false
    }
  }

  def expectToken(tokenStream: TokenStream, expect: Token): Either[SqlParsingException, Boolean] = {
    if (consumeToken(tokenStream, expect)) {
      Right(true)
    } else {
      Left(expected(expect.toString, tokenStream.peek.get))
    }
  }

  def expected(expected: String, found: TokenWithPosition): SqlParsingException =
    SqlParsingException(s"Expected ${expected}, but found: ${found}", found.position)

  def expectedButNone(expected: String): SqlParsingException =
    SqlParsingException(s"Expected ${expected}, but found EOF")

}
