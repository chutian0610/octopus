package io.octopus.sql.parser

case class SqlParsingException(message: String, cause: Throwable, val line: Int, val charPositionInLine: Int) extends RuntimeException(message, cause) {

  override def getMessage: String = s"line ${this.getLineNumber}:${this.getColumnNumber}: ${this.getErrorMessage}"

  def getLineNumber: Int = this.line

  def getColumnNumber: Int = this.charPositionInLine + 1

  def getErrorMessage: String = super.getMessage
}

object SqlParsingException {

  def apply(message: String):SqlParsingException = {
    SqlParsingException(message, null, 1, 0)
  }

  def apply(message: String,nodeLocation: Option[SqlNodePosition]):SqlParsingException = {
    if (nodeLocation.isDefined) {
      SqlParsingException(message, null, nodeLocation.get.getLineNumber, nodeLocation.get.getColumnNumber)
    } else {
      SqlParsingException(message, null, 1, 0)
    }
  }

  def apply(message: String, nodeLocation: SqlNodePosition):SqlParsingException= {
    SqlParsingException(message, null, nodeLocation.getLineNumber, nodeLocation.getColumnNumber)
  }

}