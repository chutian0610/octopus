package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.{Octopus, PrestoDB}
import io.octopus.sql.parser.token.{TokenStream, Tokens}
import org.scalatest.flatspec.AnyFlatSpec

class SqlTokenizerTest extends AnyFlatSpec {
  "tokenizer error msg" should "success" in {
    val e = SqlParsingException("test")
    val expect = "error at line [1:1] = test"
    assert(expect == e.getMessage)
  }

  "tokenizer select int" should "success" in{
    val sql = "SELECT 1"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      tokenizer.buildWord("SELECT",None),
      Tokens.space,
      Tokens.number("1",false)
    )))
  }
  "tokenizer select long" should "success" in {
    val sql = "SELECT 1L"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      tokenizer.buildWord("SELECT", None),
      Tokens.space,
      Tokens.number("1", true)
    )))
  }

  "tokenizer select float" should "success" in {
    val sql = "SELECT .1"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      tokenizer.buildWord("SELECT", None),
      Tokens.space,
      Tokens.number(".1", false)
    )))
  }
  "tokenizer select double" should "success" in {
    val sql = "SELECT 1.1"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      tokenizer.buildWord("SELECT", None),
      Tokens.space,
      Tokens.number("1.1", false)
    )))
  }

  "tokenizer select exponent" should "success" in {
    val sql = "SELECT 1e10, 1e-10, 1e+10, 1ea, 1e-10a, 1e-10-10"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      tokenizer.buildWord("SELECT", None),
      Tokens.space,
      Tokens.number("1e10", false),
      Tokens.comma,
      Tokens.space,
      Tokens.number("1e-10", false),
      Tokens.comma,
      Tokens.space,
      Tokens.number("1e+10", false),
      Tokens.comma,
      Tokens.space,
      Tokens.number("1", false),
      tokenizer.buildWord("ea", None),
      Tokens.comma,
      Tokens.space,
      Tokens.number("1e-10", false),
      tokenizer.buildWord("a", None),
      Tokens.comma,
      Tokens.space,
      Tokens.number("1e-10", false),
      Tokens.minus,
      Tokens.number("10", false)
    )))
  }
  "tokenizer scalar function" should "success" in {
    val sql = "SELECT sqrt(1)"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      tokenizer.buildWord("SELECT", None),
      Tokens.space,
      tokenizer.buildWord("sqrt", None),
      Tokens.leftParen,
      Tokens.number("1", false),
      Tokens.rightParen
    )))
  }
  "tokenizer string concat" should "success" in {
    val sql = "SELECT 'a' || 'b'"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      tokenizer.buildWord("SELECT", None),
      Tokens.space,
      Tokens.naturalString("a",'\''),
      Tokens.space,
      Tokens.concat,
      Tokens.space,
      Tokens.naturalString("b",'\'')
    )))
  }

}
