package info.victorchu.octopus.sql.parser.antlr

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.antlr.v4.runtime.atn.ATNState.{BLOCK_START, RULE_START}
import org.antlr.v4.runtime.atn.{ATN, ATNState, NotSetTransition, RuleStopState, RuleTransition, Transition, WildcardTransition}
import org.antlr.v4.runtime.misc.IntervalSet
import org.antlr.v4.runtime.{BaseErrorListener, NoViableAltException, Parser, RecognitionException,Recognizer, RuleContext, Token, TokenStream, Vocabulary}

import java.util
import java.util.stream.Collectors
import java.util.{ArrayDeque, Comparator, HashMap, HashSet, Map, Queue, Set}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
  * Antlr4 Default ErrorHandler
  */
class ErrorHandler private(val specialRules: mutable.Map[Int, String] = mutable.Map(),
                           val specialTokens: mutable.Map[Int, String] = mutable.Map(),
                           val ignoredRules: mutable.Set[Int] = mutable.Set()
                          ) extends BaseErrorListener {
  override def syntaxError(recognizer: Recognizer[?, ?], offendingSymbol: AnyRef, line: Int, charPositionInLine: Int, message: String, e: RecognitionException): Unit = {
    var msg = message
    try {
      val parser: Parser = recognizer.asInstanceOf[Parser]
      val atn: ATN = parser.getATN
      var currentState: ATNState = null
      var currentToken: Token = null
      var context: RuleContext = null
      if (e != null) {
        currentState = atn.states.get(e.getOffendingState)
        currentToken = e.getOffendingToken
        context = e.getCtx
        e match
          case exception: NoViableAltException => currentToken = exception.getStartToken
          case _ =>
      }
      else {
        currentState = atn.states.get(parser.getState)
        currentToken = parser.getCurrentToken
        context = parser.getContext
      }
      val analyzer = new ErrorHandler.Analyzer(atn, parser.getVocabulary, specialRules, specialTokens, ignoredRules, parser.getTokenStream)
      val candidates = analyzer.process(currentState, currentToken.getTokenIndex, context)
      // pick the candidate tokens associated largest token index processed (i.e., the path that consumed the most input)
      val expected = candidates.asMap().asScala.toMap.maxBy((k,v)=>k)._2.asScala.toList.sorted.mkString(",")
      msg = s"mismatched input [${offendingSymbol.asInstanceOf[Token].getText}]. Expecting: ${expected}"
    } catch {
      case exception: Exception =>
        ErrorHandler.logger.error("Unexpected failure when handling parsing error. This is likely a bug in the implementation",exception)
    }
    throw new ParsingException(msg, e, line, charPositionInLine)
  }
}

object ErrorHandler {
  import com.typesafe.scalalogging.Logger
  import scala.collection.mutable
  import scala.jdk.CollectionConverters._
  import scala.util.control.Breaks._
  private val logger = Logger(getClass.getName)

  def builder: ErrorHandler.Builder = new ErrorHandler.Builder
  class Analyzer(val atn: ATN,
                 val vocabulary: Vocabulary,
                 val specialRules: mutable.Map[Int, String],
                 val specialTokens: mutable.Map[Int, String],
                 val ignoredRules: mutable.Set[Int],
                 val stream: TokenStream) {

    def process(currentState: ATNState, tokenIndex: Int, context: RuleContext): Multimap[Int, String] = {
      process(new ErrorHandler.ParsingState(currentState, tokenIndex, makeCallStack(context)))
    }
    def process(start: ErrorHandler.ParsingState): Multimap[Int, String] = {
      val candidates: Multimap[Int, String] = HashMultimap.create
      // Simulates the ATN by consuming input tokens and walking transitions.
      // The ATN can be in multiple states (similar to an NFA)
      val activeStates: mutable.Queue[ParsingState] = new mutable.Queue[ParsingState]
      activeStates.enqueue(start)
      while (activeStates.nonEmpty) {
        breakable {
          val current: ParsingState = activeStates.dequeue()
          val state: ATNState = current.state
          val tokenIndex: Int = current.tokenIndex
          val caller: CallerContext = current.caller
          if (state.getStateType == BLOCK_START || state.getStateType == RULE_START) {
            val rule: Int = state.ruleIndex
            if (specialRules.contains(rule)) {
              candidates.put(tokenIndex, specialRules(rule))
              break
            }
            else if (ignoredRules.contains(rule)) {
              break
            }
          }
          if (state.isInstanceOf[RuleStopState]) {
            if (caller != null) {
              // continue from the target state of the rule transition in the parent rule
              activeStates.enqueue(new ParsingState(caller.followState, tokenIndex, caller.parent))
            }
            else {
              // we've reached the end of the top-level rule, so the only candidate left is EOF at this point
              candidates.putAll(tokenIndex, getTokenNames(IntervalSet.of(Token.EOF)))
            }
            break
          }
          for (i <- 0 until state.getNumberOfTransitions) {
            val transition: Transition = state.transition(i)
            transition match
              case transition1: RuleTransition => activeStates.enqueue(new ParsingState(transition.target, tokenIndex, new CallerContext(caller, transition1.followState)))
              case _ =>
                if (transition.isEpsilon) activeStates.enqueue(new ParsingState(transition.target, tokenIndex, caller))
                else if (transition.isInstanceOf[WildcardTransition]) throw new UnsupportedOperationException("not yet implemented: wildcard transition")
                else {
                var labels: IntervalSet = transition.label
                  if (transition.isInstanceOf[NotSetTransition]) {
                    labels = labels.complement(IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType))
                  }
                  val currentToken: Int = stream.get(tokenIndex).getType
                  if (labels.contains(currentToken)) {
                    activeStates.enqueue(new ParsingState(transition.target, tokenIndex + 1, caller))
                  }
                  else {
                    candidates.putAll(tokenIndex, getTokenNames(labels))
                  }
              }
          }
        }
      }
      candidates
    }

    private def getTokenNames(tokens: IntervalSet): util.Set[String] = tokens.toSet.stream.map((token: Integer) => {
      def foo(token: Integer): String = {
        if (token eq Token.EOF) return "<EOF>"
        specialTokens.getOrElse(token, vocabulary.getDisplayName(token))
      }

      foo(token)
    }).collect(Collectors.toSet)

    private def makeCallStack(context: RuleContext): CallerContext = {
      if (context == null || context.invokingState == -1) return null
      val parent: CallerContext = makeCallStack(context.parent)
      val followState: ATNState = atn.states.get(context.invokingState).transition(0).asInstanceOf[RuleTransition].followState
      new CallerContext(parent, followState)
    }
  }

  class CallerContext(val parent: CallerContext, val followState: ATNState) {}

  class ParsingState(val state: ATNState, val tokenIndex: Int, val caller: CallerContext) {
  }


  class Builder (val specialRules: mutable.Map[Int, String] = mutable.Map(),
                        val specialTokens: mutable.Map[Int, String] = mutable.Map(),
                        val ignoredRules: mutable.Set[Int] = mutable.Set()) {
    def specialRule(ruleId: Int, name: String): Builder = {
      specialRules.put(ruleId, name)
      this
    }

    def specialToken(tokenId: Int, name: String): Builder = {
      specialTokens.put(tokenId, name)
      this
    }

    def ignoredRule(ruleId: Int): Builder = {
      ignoredRules.add(ruleId)
      this
    }

    def build: ErrorHandler = new ErrorHandler(specialRules, specialTokens, ignoredRules)
  }

}
