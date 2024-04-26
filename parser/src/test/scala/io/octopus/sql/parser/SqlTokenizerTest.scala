package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.{Octopus,MySQL}
import io.octopus.sql.parser.token.{TokenStream, Tokens}
import org.scalatest.flatspec.AnyFlatSpec

class SqlTokenizerTest extends AnyFlatSpec {
  "tokenizer error msg" should "success" in {
    val e = SqlParsingException("test")
    val expect = "error at line [1:1] = test"
    assert(expect == e.getMessage)
  }

  "tokenizer select single quote string" should "success" in {
    val sql = "SELECT 'a'"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.naturalString("a", '\'')
    )))
  }
  "tokenizer select single quote string with quote escape" should "success" in {
    val sql = "SELECT 'a'''"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.naturalString("a'", '\'')
    )))
  }


  "tokenizer select double quote string" should "success" in {
    val sql = """SELECT "a""""
    val sqlDialect = MySQL()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.naturalString("a", '"')
    )))
  }

  "tokenizer select double quote string with quote escape" should "success" in {
    val sql = """SELECT "a""""""
    val sqlDialect = MySQL()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.naturalString("a\"", '"')
    )))
  }

  "tokenizer select identifier" should "success" in {
    val sql = "SELECT a"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.identifier("a", None)
    )))
  }

  "tokenizer select quote identifier" should "success" in {
    val sql = """SELECT "select""""
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.identifier("select", Some('"'))
    )))
  }

  "tokenizer select int" should "success" in{
    val sql = "SELECT 1"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      Tokens.keyWord("SELECT"),
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
      Tokens.keyWord("SELECT"),
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
      Tokens.keyWord("SELECT"),
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
      Tokens.keyWord("SELECT"),
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
      Tokens.keyWord("SELECT"),
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
      Tokens.identifier("ea", None),
      Tokens.comma,
      Tokens.space,
      Tokens.number("1e-10", false),
      Tokens.identifier("a", None),
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
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.identifier("sqrt", None),
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
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.naturalString("a",'\''),
      Tokens.space,
      Tokens.concat,
      Tokens.space,
      Tokens.naturalString("b",'\'')
    )))
  }
  "tokenizer logical operator" should "success" in {
    val sql = "SELECT true and false or true"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.keyWord("true"),
      Tokens.space,
      Tokens.keyWord("and"),
      Tokens.space,
      Tokens.keyWord("false"),
      Tokens.space,
      Tokens.keyWord("or"),
      Tokens.space,
      Tokens.keyWord("true")
    )))
  }

  "tokenizer simple select" should "success" in {
    val sql = "SELECT * FROM customer WHERE id = 1 LIMIT 5"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get == TokenStream(List(
      Tokens.keyWord("SELECT"),
      Tokens.space,
      Tokens.asterisk,
      Tokens.space,
      Tokens.keyWord("FROM"),
      Tokens.space,
      Tokens.identifier("customer", None),
      Tokens.space,
      Tokens.keyWord("WHERE"),
      Tokens.space,
      Tokens.identifier("id", None),
      Tokens.space,
      Tokens.eq,
      Tokens.space,
      Tokens.number("1", false),
      Tokens.space,
      Tokens.keyWord("LIMIT"),
      Tokens.space,
      Tokens.number("5", false)
    )))
  }

}
