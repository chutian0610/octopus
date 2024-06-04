package io.github.chutian0610.octopus.sql.parser

import io.github.chutian0610.octopus.sql.parser.dialect.SqlDialect
import io.github.chutian0610.octopus.sql.parser.token.Word.KeyWord
import io.github.chutian0610.octopus.sql.parser.token.{TokenStream, Tokens}
import io.github.chutian0610.octopus.sql.parser.tree.{Identifier, Query, QueryBody, QuerySpecification, SqlNode, SubQuery, WithClause, WithQuery}
import io.github.chutian0610.octopus.sql.parser.dialect.SqlDialect
import io.github.chutian0610.octopus.sql.parser.token.*

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
   * @param tokenStream The token stream to parse
   * @return
   */
  def parseStatements(tokenStream: TokenStream): Either[SqlParsingException, List[SqlNode]] = {
    var expecting_statement_delimiter: Boolean = false
    val list = List.newBuilder[SqlNode]
    while (!lookAheadToken(tokenStream,Tokens.eof)) {
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
   * @param tokenStream The token stream to parse
   * @return
   */
  def parseStatement(tokenStream: TokenStream): Either[SqlParsingException, SqlNode] = {
    val token = tokenStream.peekAndSkipWhitespace.map(_.token)
    token match
      case Some(w: KeyWord) => {
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

  /**
   * parse a query
   * @param tokenStream The token stream to parse
   * @return
   */
  def parseQuery(tokenStream: TokenStream): Either[SqlParsingException, Query] = {
    // parse with clause
    val withClause: Option[WithClause] = if (lookAheadKeyWord(tokenStream, KEYWORDS.WITH)) {
      val kw_with = tokenStream.nextAndSkipWhitespace.get
      val position = kw_with.position
      val recursive = consumeKeyWord(tokenStream, KEYWORDS.RECURSIVE)
      parseCommaSeparatedList[WithQuery](tokenStream, parseNamedQuery) match
        case Left(e) => return Left(e)
        case Right(namedQueries) => Some(WithClause(
          position, recursive, namedQueries
        ))
    } else {
      None
    }
    // parse query body
    
    ???
  }

  /**
   * parse query body
   * @param tokenStream The token stream to parse
   * @return
   */
  def parseQueryBody(tokenStream: TokenStream): Either[SqlParsingException, QueryBody] = {
    val queryBody = if(lookAheadKeyWord(tokenStream, KEYWORDS.SELECT)){
      parseQuerySpecification(tokenStream)
    }else if(lookAheadToken(tokenStream, Tokens.leftParen)){
      // matches subQuery
      val leftParen = tokenStream.nextAndSkipWhitespace.get
      parseQuery(tokenStream).flatMap(query => {
        expectToken(tokenStream, Tokens.rightParen).flatMap( 
          _ => Right(SubQuery(leftParen.position, query)))
      })
    }else if(lookAheadKeyWord(tokenStream, KEYWORDS.VALUES)){
      // matches Values
      ???
    }else if(lookAheadKeyWord(tokenStream, KEYWORDS.TABLE)){
      // matches Table
      ???
    }else{
      return Left(
        expected("SELECT, VALUES, TABLE, or a subQuery in the query body",
          tokenStream.peekAndSkipWhitespace
        ))
    }
    queryBody
  }
  
  
  def parseQuerySpecification(tokenStream: TokenStream): Either[SqlParsingException, QuerySpecification] = {
    import quest._
    quest{
      val select = expectKeyWord(tokenStream,KEYWORDS.SELECT).?
      ???
    }
  }

  /**
   * parse comma separated list
   * @param tokenStream The token stream to parse
   * @param itemParser item parse function
   * @tparam T SqlNode Type
   * @return
   */
  def parseCommaSeparatedList[T](tokenStream: TokenStream,
                                 itemParser: TokenStream => Either[SqlParsingException, T]): Either[SqlParsingException, List[T]] = {
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

  /**
   * parse named query in withClause
   * (`name [( col1, col2, ... )] AS (subQuery)`)
   * @param tokenStream
   * @return
   */
  def parseNamedQuery(tokenStream: TokenStream): Either[SqlParsingException, WithQuery] = {
    parseIdentifier(tokenStream).flatMap(name => {
      if (consumeKeyWord(tokenStream, KEYWORDS.AS)) {
        // parse subQuery
        expectToken(tokenStream, Tokens.leftParen).flatMap(_ => {
          parseQuery(tokenStream).flatMap(subQuery => {
            expectToken(tokenStream, Tokens.rightParen).flatMap(_ => {
              Right(WithQuery(
                position = name.position,
                name = name,
                columnNames = None,
                query = subQuery))
            })
          })
        })
      } else {
        import quest._
        quest{
          // parse column names
          val columnNameList= parseColumnNames(tokenStream).?
          expectKeyWord(tokenStream, KEYWORDS.AS).?
          expectToken(tokenStream, Tokens.leftParen).?
          // parse subQuery
          val subQuery= parseQuery(tokenStream).?
          expectToken(tokenStream, Tokens.rightParen).?
          Right(WithQuery(
                position = name.position, name = name,
                columnNames = Some(columnNameList),
                query = subQuery))

        }
      }
    })
  }

  /**
   * parse column names List
   * @param tokenStream The token stream to parse
   * @param allowEmpty allow empty list
   * @param optional List is option
   * @return
   */
  def parseColumnNames(tokenStream: TokenStream, allowEmpty: Boolean = false, optional: Boolean = true): Either[SqlParsingException, List[Identifier]] = {
    if (consumeToken(tokenStream, Tokens.leftParen)) {
      expectToken(tokenStream, Tokens.rightParen) match
        case Right(t) if allowEmpty => Right(List.empty)
        case _ =>
          parseCommaSeparatedList(tokenStream, parseIdentifier).flatMap(list => {
            expectToken(tokenStream, Tokens.rightParen).flatMap(_ => {
              Right(list)
            })
          })
    } else {
      if (optional) {
        Right(List.empty)
      } else {
        Left(expected("a list of columns in parentheses", tokenStream.peek))
      }
    }
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
      case t: Option[TokenWithPosition] => Left(expected("identifier", t))
  }

  /********************************  util functions *********************************************/
  def lookAheadKeyWord(tokenStream: TokenStream, expected: KEYWORD): Boolean = {
    tokenStream.peekAndSkipWhitespace.map(_.token) match {
      case Some(kw: Word.KeyWord) if kw.k == expected => true
      case _ => false
    }
  }

  def expectKeyWord(tokenStream: TokenStream, expect: KEYWORD): Either[SqlParsingException, TokenWithPosition] = {
    val peek =  tokenStream.peekAndSkipWhitespace
    peek.map(_.token) match
      case Some(w: Word.KeyWord) if w.k == expect => {
        // consume token
        tokenStream.nextAndSkipWhitespace
        Right(peek.get)
      }
      case _ =>  Left(expected(expect.toString, tokenStream.peek.get))
  }

  def consumeKeyWord(tokenStream: TokenStream, expected: KEYWORD): Boolean = {
    tokenStream.peekAndSkipWhitespace.map(_.token) match
      case Some(w: Word.KeyWord) if w.k == expected => {
        // consume token
        tokenStream.nextAndSkipWhitespace
        true
      }
      case _ => false
  }

  def lookAheadToken(tokenStream: TokenStream, expected: Token): Boolean = {
    val peek = tokenStream.peekAndSkipWhitespace
    if (peek.isDefined && peek.exists(t => t.sameAs(expected))) {
      true
    } else {
      false
    }
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
  def expectToken(tokenStream: TokenStream, expect: Token): Either[SqlParsingException, TokenWithPosition] = {
    val peek = tokenStream.peekAndSkipWhitespace
    if (peek.isDefined && peek.exists(t => t.sameAs(expect))) {
      tokenStream.nextAndSkipWhitespace
      Right(peek.get)
    } else {
      Left(expected(expect.toString, tokenStream.peek))
    }
  }

  def expected(expect: String, found: Option[TokenWithPosition]): SqlParsingException = {
    found match
      case Some(t) => this.expected(expect, t)
      case _ => expectedButNone(expect)
  }

  def expected(expect: String, found: TokenWithPosition): SqlParsingException =
    SqlParsingException(s"Expected ${expect}, but found: ${found}", found.position)

  def expectedButNone(expect: String): SqlParsingException =
    SqlParsingException(s"Expected ${expect}, but found EOF")

}
