package info.victorchu.octopus.sql.parser.parsec

import fastparse.*
import fastparse.internal.Msgs

object SqlWhitespace {
  implicit object whitespace extends Whitespace {
    def apply(ctx: ParsingRun[_]) = {
      var index = ctx.index
      val input = ctx.input

      while (
        input.isReachable(index) &&
          (input(index) match {
            case ' ' | '\t' | '\r' | '\n' => true
            case _ => false
          })
      ) index += 1
      if (ctx.verboseFailures) ctx.reportTerminalMsg(index, Msgs.empty)
      ctx.freshSuccessUnit(index = index)
    }
  }
}
