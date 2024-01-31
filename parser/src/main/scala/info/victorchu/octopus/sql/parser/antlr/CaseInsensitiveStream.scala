package info.victorchu.octopus.sql.parser.antlr

import org.antlr.v4.runtime.{CharStream, IntStream}
import org.antlr.v4.runtime.misc.Interval

class CaseInsensitiveStream(private val stream: CharStream) extends CharStream {
  override def getText(interval: Interval): String = stream.getText(interval)

  override def consume(): Unit = {
    stream.consume()
  }

  override def LA(i: Int): Int = {
    val result = stream.LA(i)
    result match {
      case 0 => 0
      case IntStream.EOF =>
        result
      case _ =>
        Character.toUpperCase(result)
    }
  }

  override def mark: Int = stream.mark

  override def release(marker: Int): Unit = {
    stream.release(marker)
  }

  override def index: Int = stream.index

  override def seek(index: Int): Unit = {
    stream.seek(index)
  }

  override def size: Int = stream.size

  override def getSourceName: String = stream.getSourceName
}
