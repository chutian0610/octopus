package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.SqlDialect
import io.octopus.sql.parser.token.TokenStream.expected
import io.octopus.sql.parser.token.TokenType.SEMICOLON
import io.octopus.sql.parser.token.*
import io.octopus.sql.parser.tree.SqlNode

/**
 * SqlParser
 */
class SqlParser(sqlDialect: SqlDialect,sqlParingOption: SqlParingOption = SqlParingOption()) {
  val tokenizer: SqlTokenizer = new SqlTokenizer(sqlDialect, sqlParingOption)

  def parseSql(sql: String): Either[SqlParsingException, List[SqlNode]]= {
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
  private def parseStatements(tokenStream: TokenStream): Either[SqlParsingException, List[SqlNode]]={
    var expecting_statement_delimiter: Boolean = false
    val list = List.newBuilder[SqlNode]
    while (!tokenStream.peekAndSkipWhitespace.exists(_.unWrap.equals(Tokens.eof))) {
      // ignore empty statements (between successive statement delimiters)
      while (tokenStream.consumeByTokenType(SEMICOLON)) {
        expecting_statement_delimiter = false
      }
      if(expecting_statement_delimiter){
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
  private def parseStatement(tokenStream: TokenStream): Either[SqlParsingException, SqlNode]={
    val token = tokenStream.peekAndSkipWhitespace.map(_.unWrap)
    token match
      case Some(w: Word.KeyWord)=>{
        KEYWORDS.withNameInsensitiveOption(w.text) match {
          case Some(value@(KEYWORDS.SELECT | KEYWORDS.WITH | KEYWORDS.VALUES)) => {
            // handle Query
          }
          case _ => expected("an SQL statement", tokenStream.peek.get)
        }
      }
      case _ =>
    ???
  }

  private def parseQuery(tokenStream: TokenStream):Either[SqlParsingException,SqlNode]={
    ???
  }
}
