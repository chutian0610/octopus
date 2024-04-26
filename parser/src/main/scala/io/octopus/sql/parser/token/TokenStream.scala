package io.octopus.sql.parser.token

case class TokenStream(tokens: List[Token], var current: Int) {

  def peek: Option[Token] = tokens.lift(current)

  def next: Option[Token] = {
    if (current < tokens.length) {
      current += 1
      tokens.lift(current)
    } else {
      None
    }
  }

  /**
   *  Consume the next token if it matches the expected token, otherwise return false
   * @param t the expected token
   * @return
   */
  def consume(t:Token): Boolean = {
    peek match
      case Some(token) if token == t =>
        next
        true

      case _ =>false
  }

  override def equals(obj: Any): Boolean = obj match
    case TokenStream(otherTokens, _) => tokens == otherTokens
    case _ => false
}

object TokenStream {
  def apply(tokens: List[Token]): TokenStream = TokenStream(tokens, 0)
}
