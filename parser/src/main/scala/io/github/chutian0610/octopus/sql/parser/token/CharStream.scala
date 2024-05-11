package io.github.chutian0610.octopus.sql.parser.token

import io.github.chutian0610.octopus.sql.parser.Position

import scala.util.control.Breaks.{break, breakable}

case class CharStream(text: String, var current: Int, var line: Int, var column: Int) {
  def peek: Option[Char] = text.lift(current)

  def next: Option[Char] = {
      val c = text.lift(current)
      c match {
        case Some(c) => {
          current += 1
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
  }

  def next(n: Int):Option[String] ={
    val list = List.newBuilder[Char]
    for (i <- 0 until n) {
      val c= next
      if(c.isDefined){
        list.addOne(c.get)
      }
    }
    if(list.result().isEmpty){
      None
    }else{
      Some(list.result().mkString(""))
    }
    
  }
  def peekCharsWhile(predicate: Char => Boolean): String = {
    val stringBuilder = new StringBuilder()
    breakable {
      while (peek.isDefined) {
        val ch = peek.get
        if (predicate(ch)) {
          next
          stringBuilder.append(ch)
        } else {
          break
        }
      }
    }
    stringBuilder.toString()
  }

  def position: Position = Position(line, column)
}

object CharStream {
  def apply(text: String): CharStream = CharStream(text, 0, 0, 0)
}
