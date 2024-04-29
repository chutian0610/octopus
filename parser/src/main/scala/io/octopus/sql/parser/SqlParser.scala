package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.SqlDialect
import io.octopus.sql.parser.token.{TokenStream, Tokens}
import io.octopus.sql.parser.token.TokenType.{SEMICOLON, TAB}
import io.octopus.sql.parser.tree.SqlNode

/**
 * SqlParser
 */
class SqlParser(sqlDialect: SqlDialect,sqlParingOption: SqlParingOption = SqlParingOption()) {
  val tokenizer: SqlTokenizer = new SqlTokenizer(sqlDialect, sqlParingOption)

  def parseSql(sql: String): Either[SqlParsingException, SqlNode]= {
    // sql -> token stream
    val tokenized = tokenizer.tokenize(sql)
    tokenized match {
      case Left(e) => Left(e)
      case Right(tokens) => parse(tokens) // token stream -> AST
    }
  }
  // ============================== parser functions ===============================
  /**
   * Parse the token stream into an AST
   * @param tokenStream The token stream to parse
   * @return
   */
  def parse(tokenStream: TokenStream): Either[SqlParsingException, SqlNode]={
    var expecting_statement_delimiter: Boolean = false
    Right(???)
  }
}
