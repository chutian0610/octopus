package io.github.chutian0610.octopus.sql.parser.dialect

import io.github.chutian0610.octopus.common.util.Engine
import io.github.chutian0610.octopus.sql.parser.token.KEYWORD

abstract class SqlDialect(engine: Engine
                     ) {
  def dialectOf(engines: Engine*):Boolean = {
    engines.contains(engine)
  }

  def partOfIdentifier(c: Char):Boolean

  def startOfIdentifier(c: Char):Boolean

  def startOfDelimitedIdentifier(c: Char): Boolean

  def matchKeyWord(s: String): Option[KEYWORD]

  def matchReservedWords(s :String): Option[String]
}
