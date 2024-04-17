package io.octopus.sql.parser.dialect

import com.google.common.base.CharMatcher
import io.octopus.sql.utils.Engine.{MYSQL, PRESTO_DB}

class MySQL extends SqlDialect(engine = MYSQL) {

  override def partOfIdentifier(c: Char): Boolean = {
    startOfIdentifier(c)
      || CharMatcher.digit().matches(c)
  }

  override def startOfIdentifier(c: Char): Boolean = {
    CharMatcher.javaLetter().matches(c)
      || c == '_'
      || c == '@'
      || c == '$'
      || CharMatcher.inRange ('\u0080','\uffff').matches(c)
  }

  override def startOfQuotedIdentifier(c: Char): Boolean = {
    c == '`'
  }
}
