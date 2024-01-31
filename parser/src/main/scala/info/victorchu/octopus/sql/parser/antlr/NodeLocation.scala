package info.victorchu.octopus.sql.parser.antlr

case class NodeLocation(line: Int, charPositionInLine: Int) {
  def getLineNumber: Int = this.line
  def getColumnNumber: Int = this.charPositionInLine + 1
}

