package info.victorchu.octopus.sql.parser.antlr

import com.google.common.base.Preconditions
import org.antlr.v4.runtime.{Lexer, Parser}
import org.antlr.v4.runtime.atn.{ATN, LexerATNSimulator, ParserATNSimulator, PredictionContextCache}
import org.antlr.v4.runtime.dfa.DFA

import java.util.Objects.requireNonNull

object AntlrATNCacheFields {
  private def createDecisionToDFA(atn: ATN) = {
    val decisionToDFA = new Array[DFA](atn.getNumberOfDecisions)
    for (i <- decisionToDFA.indices) {
      decisionToDFA(i) = new DFA(atn.getDecisionState(i), i)
    }
    decisionToDFA
  }
}

final class AntlrATNCacheFields(atn: ATN) {
  requireNonNull(atn, "atn is null")
  private val predictionContextCache = new PredictionContextCache
  private val decisionToDFA = AntlrATNCacheFields.createDecisionToDFA(atn)

  @SuppressWarnings(Array("ObjectEquality"))
  def configureLexer(lexer: Lexer): Unit = {
    requireNonNull(lexer, "lexer is null")
    // Intentional identity equals comparison
    Preconditions.checkArgument(atn eq lexer.getATN, "Lexer ATN mismatch: expected %s, found %s", atn, lexer.getATN)
    lexer.setInterpreter(new LexerATNSimulator(lexer, atn, decisionToDFA, predictionContextCache))
  }

  @SuppressWarnings(Array("ObjectEquality"))
  def configureParser(parser: Parser): Unit = {
    requireNonNull(parser, "parser is null")
    // Intentional identity equals comparison
    Preconditions.checkArgument(atn eq parser.getATN, "Parser ATN mismatch: expected %s, found %s", atn, parser.getATN)
    parser.setInterpreter(new ParserATNSimulator(parser, atn, decisionToDFA, predictionContextCache))
  }
}

