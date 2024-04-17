package io.octopus.sql.parser.dialect

import com.google.common.base.CharMatcher
import io.octopus.sql.utils.Engine.PRESTO_DB

class PrestoDB extends SqlDialect(engine = PRESTO_DB) {

  override def partOfIdentifier(c: Char): Boolean = {
    CharMatcher.javaLetterOrDigit().matches(c)
      || c == '_'
      || c == '@'
      || c == ':'
  }

  override def startOfIdentifier(c: Char): Boolean = {
    CharMatcher.javaLetterOrDigit().matches(c)
      || c == '_'
  }

  override def startOfQuotedIdentifier(c: Char): Boolean = {
    c == '"'
  }
}
