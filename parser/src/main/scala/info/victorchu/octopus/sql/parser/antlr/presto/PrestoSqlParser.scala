package info.victorchu.octopus.sql.parser.antlr.presto

import info.victorchu.octopus.sql.parser.{SqlNodePosition, SqlParser, SqlParsingException}
import info.victorchu.octopus.sql.parser.antlr.*
import info.victorchu.octopus.sql.tree.SqlNode
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.{Pair, ParseCancellationException}
import org.antlr.v4.runtime.tree.TerminalNode

import java.util
import java.util.Objects.requireNonNull
import java.util.function.Function
import scala.jdk.CollectionConverters.*

object PrestoSqlParser {

  def apply():PrestoSqlParser={
    new PrestoSqlParser(PrestoSqlParserOption())
  }
  private val LEXER_ERROR_LISTENER: BaseErrorListener = new BaseErrorListener() {
    override def syntaxError(recognizer: Recognizer[_, _], offendingSymbol: AnyRef, line: Int, charPositionInLine: Int, message: String, e: RecognitionException): Unit = {
      throw new SqlParsingException(message, e, line, charPositionInLine)
    }
  }
  private val PARSER_ERROR_HANDLER: AntlrErrorHandler = AntlrErrorHandler.builder
          .specialRule(PrestoParser.RULE_expression, "<expression>")
          .specialRule(PrestoParser.RULE_booleanExpression, "<expression>")
          .specialRule(PrestoParser.RULE_valueExpression, "<expression>")
          .specialRule(PrestoParser.RULE_primaryExpression, "<expression>")
          .specialRule(PrestoParser.RULE_identifier, "<identifier>")
          .specialRule(PrestoParser.RULE_string, "<string>")
          .specialRule(PrestoParser.RULE_query, "<query>")
          .specialRule(PrestoParser.RULE_type, "<type>")
          .specialToken(PrestoParser.INTEGER_VALUE, "<integer>")
          .ignoredRule(PrestoParser.RULE_nonReserved).build

  class PostProcessor(ruleNames: List[String],
                      options: PrestoSqlParserOption
                     ) extends PrestoBaseListener {
    override def exitUnquotedIdentifier(context: PrestoParser.UnquotedIdentifierContext): Unit = {
      val identifier: String = context.IDENTIFIER.getText
      for (symbol <- options.allowedIdentifierSymbols) {
        if (identifier.indexOf(symbol) >= 0) {
          throw new SqlParsingException("identifiers must not contain '" + symbol + "'", SqlNodePosition(context.IDENTIFIER.getSymbol.getLine, context.IDENTIFIER.getSymbol.getCharPositionInLine))
        }
      }
    }

    override def exitBackQuotedIdentifier(context: PrestoParser.BackQuotedIdentifierContext): Unit = {
      val token: Token = context.BACKQUOTED_IDENTIFIER.getSymbol
      throw new SqlParsingException("backquoted identifiers are not supported; use double quotes to quote identifiers", SqlNodePosition(token.getLine,token.getCharPositionInLine))
    }

    // 改造，支持数字开头标识符
    override def exitDigitIdentifier(context: PrestoParser.DigitIdentifierContext): Unit = {
      val token: Token = context.DIGIT_IDENTIFIER.getSymbol
      throw new SqlParsingException("identifiers must not start with a digit; surround the identifier with double quotes",SqlNodePosition(token.getLine,token.getCharPositionInLine))
    }

    override def exitNonReserved(context: PrestoParser.NonReservedContext): Unit = {
      // we can't modify the tree during rule enter/exit event handling unless we're dealing with a terminal.
      // Otherwise, ANTLR gets confused an fires spurious notifications.
      if (!context.getChild(0).isInstanceOf[TerminalNode]) {
        val rule: Int = context.getChild(0).asInstanceOf[ParserRuleContext].getRuleIndex
        throw new AssertionError("nonReserved can only contain tokens. Found nested rule: " + ruleNames(rule))
      }
      // replace nonReserved words with IDENT tokens
      context.getParent.removeLastChild()
      val token: Token = context.getChild(0).getPayload.asInstanceOf[Token]
      context.getParent.addChild(new CommonToken(new Pair[TokenSource, CharStream](token.getTokenSource, token.getInputStream), PrestoLexer.IDENTIFIER, token.getChannel, token.getStartIndex, token.getStopIndex))
    }
  }
}

class PrestoSqlParser (options: PrestoSqlParserOption) extends SqlParser(options){
  requireNonNull(options, "options is null")
  val initializer:RefreshableParserInitializer = new RefreshableParserInitializer(()=>(new AntlrATNCacheFields(PrestoLexer._ATN),new AntlrATNCacheFields(PrestoParser._ATN)))

  def createStatement(sql: String): ParserRuleContext = invokeParser("statement", sql,  x=>x.singleStatement())
  def createExpression(expression: String): ParserRuleContext = invokeParser("expression", expression,  x=>x.standaloneExpression())
  def createPathSpecification(expression: String): ParserRuleContext = invokeParser("path specification", expression, x=>x.standalonePathSpecification())
  private def invokeParser(name: String, sql: String, parseFunction: Function[PrestoParser, ParserRuleContext]): ParserRuleContext = try {
    val lexer: PrestoLexer = new PrestoLexer(new CaseInsensitiveStream(CharStreams.fromString(sql)))
    val tokenStream: CommonTokenStream = new CommonTokenStream(lexer)
    val parser: PrestoParser = new PrestoParser(tokenStream)

    initializer.accept(lexer, parser)
    // Override the default error strategy to not attempt inserting or deleting a token.
    // Otherwise, it messes up error reporting
    parser.setErrorHandler(new DefaultErrorStrategy() {
      @throws[RecognitionException]
      override def recoverInline(recognizer: Parser): Token = {
        if (nextTokensContext == null) throw new InputMismatchException(recognizer)
        else throw new InputMismatchException(recognizer, nextTokensState, nextTokensContext)
      }
    })
    parser.addParseListener(new PrestoSqlParser.PostProcessor(parser.getRuleNames.toList,options))
    lexer.removeErrorListeners()
    lexer.addErrorListener(PrestoSqlParser.LEXER_ERROR_LISTENER)
    parser.removeErrorListeners()
    if (options.enhancedErrorHandlerEnabled){
      parser.addErrorListener(PrestoSqlParser.PARSER_ERROR_HANDLER)
    }
    else {
      parser.addErrorListener(PrestoSqlParser.LEXER_ERROR_LISTENER)
    }
    var tree: ParserRuleContext = null
    try {
      // first, try parsing with potentially faster SLL mode
      parser.getInterpreter.setPredictionMode(PredictionMode.SLL)
      tree = parseFunction.apply(parser)
    } catch {
      case ex: ParseCancellationException =>

        // if we fail, parse with LL mode
        // rewind input stream
        tokenStream.seek(0)

        parser.reset()
        parser.getInterpreter.setPredictionMode(PredictionMode.LL)
        tree = parseFunction.apply(parser)
    }
    tree
  } catch {
    case e: StackOverflowError =>
      throw new SqlParsingException(name + " is too large (stack overflow while parsing)")
  }
  override def parse(sql: String): Either[SqlNode, String] = {
    createStatement(sql)
    Right("TODO")
  }
}