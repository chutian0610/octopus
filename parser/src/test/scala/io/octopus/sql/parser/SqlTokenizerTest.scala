package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.PrestoDB
import io.octopus.sql.parser.token.{TokenStream, Tokens}
import org.scalatest.flatspec.AnyFlatSpec

class SqlTokenizerTest extends AnyFlatSpec {
  "tokenizer error msg" should "success" in {
    val e = SqlParsingException("test")
    val expect = "error at line [1:1] = test"
    assert(expect == e.getMessage)
  }

  "tokenizer select 1" should "success" in{
    val sql = "SELECT 1"
    val sqlDialect = PrestoDB()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      tokenizer.buildWord("SELECT",None),
      Tokens.space,
      Tokens.number("1",false)
    )))
  }
}
