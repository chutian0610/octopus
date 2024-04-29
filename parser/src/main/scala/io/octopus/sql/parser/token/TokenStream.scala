package io.octopus.sql.parser.token

case class TokenStream(tokens: List[Token], var current: Int) {

  /**
   * peek next token
   * @return
   */
  def peek: Option[Token] = tokens.lift(current)

  /**
   * get the next token
   * @return
   */
  def next: Option[Token] = {
      val t= tokens.lift(current)
      if(t.isDefined) {
        current += 1
      }
      t
  }

  /**
   * Consume the next token if it matches the expected token, otherwise return false
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

  /**
   * Consume the next token if it matches the expected token Type, otherwise return false
   * @param t
   * @return
   */
  def consumeByTokenType(tokenType: TokenType): Boolean = {
    peek match
      case Some(token) if token.tokenType == tokenType =>
        next
        true
      case _ =>false
  }
  /**
   * Consume the next token if it matches type of the expected token, otherwise return false
   * @param t
   * @return
   */
  def consumeByTokenType(token: Token): Boolean = {
    consumeByTokenType(token.tokenType)
  }

  override def equals(obj: Any): Boolean = obj match
    case TokenStream(otherTokens, _) => tokens == otherTokens
    case _ => false
}

object TokenStream {
  def apply(tokens: List[Token]): TokenStream = TokenStream(tokens, 0)
}
