package io.octopus.sql.parser.token

case class TokenStream(tokens: List[TokenWithPosition], var current: Int) {
  def peek(): Option[TokenWithPosition] = tokens.lift(current)

  def next(): Option[TokenWithPosition] = {
    if (current < tokens.length) {
      current += 1
      tokens.lift(current)
    } else {
      None
    }
  }
}

object TokenStream {
  def apply(tokens: List[TokenWithPosition]): TokenStream = TokenStream(tokens, 0)
}
