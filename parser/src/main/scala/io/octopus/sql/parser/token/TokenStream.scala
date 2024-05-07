package io.octopus.sql.parser.token

import io.octopus.sql.parser.SqlParsingException

import scala.annotation.tailrec

class TokenStream(val tokens: List[TokenWithPosition], var current: Int) {
  /**
   * peek next n token
   * @return
   */
  def peek(n:Int): Option[TokenWithPosition] = {
    tokens.lift(current + n)
  }

  def peek: Option[TokenWithPosition] =peek(0)

  def peekAndSkipWhitespace: Option[TokenWithPosition] =peekAndSkipWhitespace(0)

  def peekAndSkipWhitespace(n:Int):Option[TokenWithPosition] = {
    var index = current
    var target = n
    var token = tokens.lift(index)
    while (token.isDefined){
      if( token.get.unWrap.isInstanceOf[WhiteSpace]){
        // skip whitespace
        index += 1
      }else{
        index +=1
        if (target == 0){
          return token
        }
        target = target-1
      }
      token = tokens.lift(index)
    }
    None
  }

  /**
   * get the next token
   * @return
   */
  def next: Option[TokenWithPosition] = {
      val t= tokens.lift(current)
      if(t.isDefined) {
        current += 1
      }
      t
  }

  def nextAndSkipWhitespace: Option[TokenWithPosition] = {
    var token = tokens.lift(current)
    while (token.isDefined) {
      if (token.get.unWrap.isInstanceOf[WhiteSpace]) {
        // skip whitespace
        current += 1
      } else {
        current += 1
        return token
      }
      token = tokens.lift(current)
    }
    None
  }

  def prev: Option[TokenWithPosition] = {
    if(current>0){
      current -= 1
      tokens.lift(current)
    }else{
      None
    }
  }

  def prevAndSkipWhitespace: Option[TokenWithPosition] = {
    while (current>0) {
      var token = tokens.lift(current-1)
      if (token.get.unWrap.isInstanceOf[WhiteSpace]) {
        // skip whitespace
        current -= 1
      } else {
        current -= 1
        return token
      }
      token = tokens.lift(current)
    }
    None
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
  def apply(tokens: List[TokenWithPosition]): TokenStream =new TokenStream(tokens, 0)
  def of(tokens: List[Token]): TokenStream =new TokenStream(tokens.map(TokenWithPosition(_)), 0)
}
