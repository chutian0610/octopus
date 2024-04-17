package io.octopus.sql.parser.token

import io.octopus.sql.parser.Position

case class CharStream(text: String, var current: Int,var line:Int,var column:Int) {
  def peek(): Option[Char] = text.lift(current)

  def next(): Option[Char] = {
    if(current < text.length){
      current += 1
      val c = text.lift(current)
      c match {
        case Some(c) => {
          if (c == '\n') {
            line += 1
            column = 0
          } else {
            column += 1
          }
        }
        case None =>
      }
      c
    }else {
      None
    }
  }

  def position: Position = Position(line, column)
}

object CharStream{
  def apply(text: String): CharStream = CharStream(text, 0, 0, 0)
}
