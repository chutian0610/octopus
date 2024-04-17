package io.octopus.sql.parser.token

import io.octopus.sql.parser.token.TokenStream

case class TokenStream(tokens: List[Token], var current: Int) {
  def peek(): Option[Token] = tokens.lift(current)

  def next(): Option[Token] = {
    if(current < tokens.length){
      current += 1
      tokens.lift(current)
    }else {
      None
    }
  }
}

object TokenStream{
  def apply(tokens: List[Token]): TokenStream = TokenStream(tokens, 0)
}
