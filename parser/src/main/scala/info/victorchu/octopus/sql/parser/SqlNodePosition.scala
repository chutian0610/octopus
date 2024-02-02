package info.victorchu.octopus.sql.parser

/**
 * SqlNode Position
 * @param line line number [1,n]
 * @param column char in line position [0,n-1]
 */
case class SqlNodePosition(line : Int, column : Int){
  def getLineNumber: Int = this.line
  def getColumnNumber: Int = this.column + 1
}
