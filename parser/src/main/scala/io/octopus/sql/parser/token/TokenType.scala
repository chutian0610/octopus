package io.octopus.sql.parser.token
enum TokenType {
  case
  // Literal
    HEX_STRING
  , NATURAL_String
  , NUMBER
  // Word
  , IDENTIFIER
  , KEYWORD
  // EOF
  , EOF

  // WhiteSpace
  , SPACE
  , NEW_LINE
  , TAB
  , SINGLE_LINE_COMMENT
  , MULTI_LINE_COMMENT
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
  , DIVIDE // `/`
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
