package info.victorchu.octopus.sql.parser.parsec

trait SqlToken {
  abstract class Token {
    def text: String
  }
  case class ErrorToken(text: String) extends Token
  case class Keyword(text: String) extends Token



}
