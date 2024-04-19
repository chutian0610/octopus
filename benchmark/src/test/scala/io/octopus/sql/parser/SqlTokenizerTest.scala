package io.octopus.sql.parser

import org.scalatest.flatspec.AnyFlatSpec

class SqlTokenizerTest extends AnyFlatSpec {
  "tokenizer error" should "return" in {
    val e = SqlParsingException("test")
    val expect = "error at line [1:1] = test"
    assert(expect == e.getMessage)
  }

}
