package io.octopus.sql.parser.token

import scala.annotation.tailrec

class TokenStream(val tokens: List[Token], var current: Int) {
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

  def atEnd: Boolean = peek.isEmpty

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

  /**
   * compare two token stream, return true if their tokens are same
   * @param obj the other token stream
   * @return
   */
  def sameAs(obj: TokenStream): Boolean = {
    @tailrec def listSameAs(a: List[Token], b: List[Token]): Boolean =
      (a eq b)  // same reference
        || {    // same content
        val aEmpty = a == null || a.isEmpty
        val bEmpty = b == null || b.isEmpty
        if (!(aEmpty || bEmpty) && a.head.sameAs(b.head)) {
          listSameAs(a.tail, b.tail)
        }
        else {
          aEmpty && bEmpty
        }
      }
    if(obj eq this){
      return true
    }
    listSameAs(this.tokens, obj.tokens)
  }
}

object TokenStream {
  def apply(tokens: List[Token]): TokenStream =new TokenStream(tokens, 0)
}
