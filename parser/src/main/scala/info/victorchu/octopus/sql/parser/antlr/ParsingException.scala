package info.victorchu.octopus.sql.parser.antlr

import org.antlr.v4.runtime.RecognitionException

class ParsingException(message: String, cause: RecognitionException, val line: Int, val charPositionInLine: Int) extends RuntimeException(message, cause) {
  def this(message: String) = {
    this(message, null, 1, 0)
  }

  def this(message: String, nodeLocation: GrammarNodeLocation)={
    this(message, null, nodeLocation.getLineNumber, nodeLocation.getColumnNumber)
  }

  override def getMessage: String = s"line ${this.getLineNumber}:${this.getColumnNumber}: ${this.getErrorMessage}"

  def getLineNumber: Int = this.line

  def getColumnNumber: Int = this.charPositionInLine + 1

  def getErrorMessage: String = super.getMessage
}
