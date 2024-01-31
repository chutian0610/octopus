package info.victorchu.octopus.sql.parser.antlr

import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Parser
object RefreshableParserInitializer{
}
class RefreshableParserInitializer(supplier: ()=>(AntlrATNCacheFields ,AntlrATNCacheFields)) {
  def accept(lexer: Lexer, parser: Parser): Unit = {
    val origin: (AntlrATNCacheFields ,AntlrATNCacheFields) = this.supplier.apply()
    origin._1.configureLexer(lexer)
    origin._2.configureParser(parser)
  }
}
