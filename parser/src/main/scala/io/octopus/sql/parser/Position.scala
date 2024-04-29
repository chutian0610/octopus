package io.octopus.sql.parser

/**
 * SqlNode Position
 *
 * @param line   line number [1,n]
 * @param column char in line position [0,n-1]
 */
case class Position(line: Int, column: Int) {
  def getLineNumber: Int = this.line
  def getColumnNumber: Int = this.column + 1
  override def toString: String = s"position(${this.getLineNumber}:${this.getColumnNumber})"
}

object Position{
  def apply(tuple2: (Int,Int)): Position = Position(tuple2._1, tuple2._2)
}
