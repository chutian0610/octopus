package io.octopus.sql.parser.dialect

import io.octopus.sql.utils.Engine

abstract class SqlDialect(engine: Engine
                     ) {
  def dialectOf(engines: Engine*):Boolean = {
    engines.contains(engine)
  }
}
