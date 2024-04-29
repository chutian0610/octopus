package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.SqlDialect
import io.octopus.sql.parser.token.{Token, TokenStream, TokenWithPosition, Tokens}
import io.octopus.sql.parser.token.TokenType.{SEMICOLON, TAB}
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
      case Right(tokens) => parse(tokens) // token stream -> AST
    }
  }
  // ============================== parser functions ===============================
  /**
   * Parse the token stream into AST
   * @param tokenStream The token stream to parse
   * @return
   */
  def parse(tokenStream: TokenStream): Either[SqlParsingException, List[SqlNode]]={
    var expecting_statement_delimiter: Boolean = false
    val list = List.newBuilder[SqlNode]
    while (!tokenStream.atEnd) {
      // ignore empty statements (between successive statement delimiters)
      while (tokenStream.consumeByTokenType(SEMICOLON)) {
        expecting_statement_delimiter = false
      }


    }

    Right(list.result())
  }
  // utils functions
  def expected(expected : String, found: Token): SqlParsingException = {
    val msg = s"Expected ${expected}, but found: ${found}"
    found match
      case tokenWithPosition: TokenWithPosition =>
        SqlParsingException(msg, tokenWithPosition.position)
      case _ =>
        SqlParsingException(msg)
  }
}
