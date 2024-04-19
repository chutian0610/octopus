package io.octopus.sql.parser

case class SqlParsingException(message: String, cause: Throwable, position: Position) extends RuntimeException(message, cause) {
  override def getMessage: String = s"error at line [${position.getLineNumber}:${position.getColumnNumber}] = ${this.message}"
}

object SqlParsingException {
  def apply(message: String): SqlParsingException = {
    SqlParsingException(message, null, Position(1, 0))
  }

  def apply(message: String, position: Option[Position]): SqlParsingException = {
    if (position.isDefined) {
      SqlParsingException(message, null, position.get)
    } else {
      SqlParsingException(message, null, Position(1, 0))
    }
  }

  def apply(message: String, position: Position): SqlParsingException = {
    SqlParsingException(message, null, position)
  }

  def apply(sqlParsingException: SqlParsingException,position: Position): SqlParsingException = {
    SqlParsingException(sqlParsingException.message,sqlParsingException.cause, position)
  }

}