package io.octopus.sql.parser.token
enum TokenType {
  case
  // Literal
  HEX_STRING
  , SINGLE_QUOTED_STRING
  , DOUBLE_QUOTED_STRING
  , LONG
  , DOUBLE
  , DECIMAL
  , BINARY
  // Word
  , IDENTIFIER
  , KEYWORD
  // EOF
  , EOF

  // WhiteSpace
  , SPACE
  , NEW_LINE
  , TAB
  // Symbol
  , EQ // `=`
  , NEQ // `<>` or `!=`
  , LT // `<`
  , LTE // `<=`
  , GT // `>`
  , GTE // `>=`
  , PLUS // `+`
  , MINUS // `-`
  , ASTERISK // `*`
  , DIV // `/`
  , PERCENT // `%`
  , CONCAT // `||`
  , LEFT_PAREN // `(`
  , RIGHT_PAREN // `)`
  , LEFT_BRACKET // `[`
  , RIGHT_BRACKET // `]`
  , LEFT_BRACE // `{`
  , RIGHT_BRACE // `}`
  , COMMA // `,`
  , DOT // `.`
  , SEMICOLON // `;`
  , COLON // `:`
  , DOUBLE_COLON // `::`
  , AT // `@`
  , QUESTION // `?`
  , ARROW // `->` or `=>`
}
