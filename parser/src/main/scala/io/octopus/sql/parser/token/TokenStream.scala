package io.octopus.sql.parser.token

case class TokenStream[T](tokens: List[T], var current: Int) {
  def peek(): Option[T] = tokens.lift(current)

  def next(): Option[T] = {
    if (current < tokens.length) {
      current += 1
      tokens.lift(current)
    } else {
      None
    }
  }

  override def equals(obj: Any): Boolean = obj match
    case TokenStream(otherTokens, _) => tokens == otherTokens
    case _ => false
}

object TokenStream {
  def apply[T](tokens: List[T]): TokenStream[T] = TokenStream(tokens, 0)
}
