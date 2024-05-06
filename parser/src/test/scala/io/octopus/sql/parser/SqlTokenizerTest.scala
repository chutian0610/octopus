package io.octopus.sql.parser

import io.octopus.sql.parser.dialect.{MySQL, Octopus, PrestoDB}
import io.octopus.sql.parser.token.{KEYWORDS, TokenStream, Tokens}
import org.scalatest.flatspec.AnyFlatSpec

class SqlTokenizerTest extends AnyFlatSpec {
  "tokenizer error msg" should "success" in {
    val e = SqlParsingException("test")
    val expect = "error at line [1:1] = test"
    assert(expect == e.getMessage)
  }

  "tokenizer single line comment" should "success" in {
    val sql = "SELECT -- begin\n"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get sameAs TokenStream.of(List(
      Tokens.keyWord(KEYWORDS.SELECT),
      Tokens.space,
      Tokens.singleLineComment(" begin\n", "--")

    )))
  }
  "tokenizer single line comment at eof" should "success" in {
    val sql = "SELECT -- begin"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get sameAs TokenStream.of(List(
      Tokens.keyWord(KEYWORDS.SELECT),
      Tokens.space,
      Tokens.singleLineComment(" begin", "--")

    )))
  }
  "tokenizer multi line comment" should "success" in {
    val sql = "SELECT /*multi-line\n* /comment*/ a"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get sameAs TokenStream.of(List(
      Tokens.keyWord(KEYWORDS.SELECT),
      Tokens.space,
      Tokens.multiLineComment("multi-line\n* /comment", "/*","*/"),
      Tokens.space,
      Tokens.identifier("a", None)
    )))
  }
  "tokenizer multi line comment with more asterisk" should "success" in {
    val sql = "SELECT /** Comment **/ a"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
      Tokens.space,
      Tokens.multiLineComment("* Comment *", "/*", "*/"),
      Tokens.space,
      Tokens.identifier("a", None)
    )))
  }
  "tokenizer nested multi line comment" should "success" in {
    val sql = "SELECT /*multi-line\n* \n/* comment \n /*comment*/*/ */ /comment*/ a"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
      Tokens.space,
      Tokens.multiLineComment("multi-line\n* \n/* comment \n /*comment*/*/ */ /comment", "/*", "*/"),
      Tokens.space,
      Tokens.identifier("a", None)
    )))
  }

  "tokenizer select single quote string" should "success" in {
    val sql = "SELECT 'a'"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
      Tokens.space,
      Tokens.naturalString("a\"", '"')
    )))
  }

  "tokenizer select unicode string" should "success" in {
    val sql = "SELECT U&'Hello winter #2603 !' UESCAPE '#'"
    val sqlDialect = PrestoDB()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
      Tokens.space,
      Tokens.unicodeString("Hello winter #2603 !", '\''),
      Tokens.space,
      Tokens.keyWord(KEYWORDS.UESCAPE),
      Tokens.space,
      Tokens.naturalString("#", '\'')
    )))
  }

  "tokenizer select identifier" should "success" in {
    val sql = "SELECT a"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
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
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
      Tokens.space,
      Tokens.keyWord(KEYWORDS.TRUE),
      Tokens.space,
      Tokens.keyWord(KEYWORDS.AND),
      Tokens.space,
      Tokens.keyWord(KEYWORDS.FALSE),
      Tokens.space,
      Tokens.keyWord(KEYWORDS.OR),
      Tokens.space,
      Tokens.keyWord(KEYWORDS.TRUE)
    )))
  }

  "tokenizer simple select" should "success" in {
    val sql = "SELECT * FROM customer WHERE id = 1 LIMIT 5"
    val sqlDialect = Octopus()
    val tokenizer = SqlTokenizer(sqlDialect)
    val tokens = tokenizer.tokenize(sql)
    assert(tokens.isRight)
    assert(tokens.toOption.get sameAs TokenStream.of(List(
       Tokens.keyWord(KEYWORDS.SELECT),
      Tokens.space,
      Tokens.asterisk,
      Tokens.space,
      Tokens.keyWord(KEYWORDS.FROM),
      Tokens.space,
      Tokens.identifier("customer", None),
      Tokens.space,
      Tokens.keyWord(KEYWORDS.WHERE),
      Tokens.space,
      Tokens.identifier("id", None),
      Tokens.space,
      Tokens.eq,
      Tokens.space,
      Tokens.number("1", false),
      Tokens.space,
      Tokens.keyWord(KEYWORDS.LIMIT),
      Tokens.space,
      Tokens.number("5", false)
    )))
  }

}
