package info.victorchu.octopus.sql.parser.antlr.presto

import com.typesafe.scalalogging.Logger
import info.victorchu.octopus.sql.parser.antlr.ErrorHandler.getClass
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.tree.{RuleNode, TerminalNode}
object PrestoAntlr4Printer{
  private val logger = Logger(getClass.getName)
}
class PrestoAntlr4Printer extends PrestoBaseVisitor[String]{
  private def getLogPrefix(ruleContext: RuleContext) = {
    val sb = new StringBuffer
    var depth = ruleContext.depth
    while (depth > 0) {
      sb.append("-")
      depth -= 1
    }
    sb.append("|")
    sb.toString
  }

  private def logTrace(node: RuleNode): Unit = {
    PrestoAntlr4Printer.logger.debug(getLogPrefix(node.getRuleContext) + node.getClass.getSimpleName + "||" + node.getText)
  }

  override def visitTerminal(node: TerminalNode): String = node.getText

  override protected def aggregateResult(aggregate: String, nextResult: String): String = {
    if (aggregate == null) return nextResult
    aggregate + " " + nextResult
  }

  override def visitChildren(node: RuleNode): String = {
    logTrace(node)
    super.visitChildren(node)
  }
}
