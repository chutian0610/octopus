//package info.victorchu.octopus.sql.parser.parsec.dialect
//
//import fastparse.*
//import info.victorchu.octopus.sql.parser.parsec.SqlWhitespace.*
//
//object PrestoSqlParser {
//
////  def singleStatement[$: P] = P(
////    statement ~ End
////  )
////
////  def statement[$: P] = P(
////    queryStatement
////  )
////
////  def queryStatement[$: P] = P(
////    withClause.? ~ queryNoWith
////  )
////
////  def withClause[$: P] = P(
////    Keywords("WITH") ~/ Keywords("RECURSIVE").? ~ namedQuery.rep(min = 1, sep = ",")
////  )
////
////  def namedQuery[$: P] = P(Pass)
////
////  def queryNoWith[$: P] = P(
////    queryTerm
////      ~ (Keywords("ORDER") ~/ Keywords("BY") ~/ sortItem.rep(min = 1, sep = ",")).?
////      ~ (Keywords("OFFSET") ~ integerLiteral).?
////      ~ (Keywords("LIMIT") ~ integerLiteral).?
////  )
////  def queryTerm[$: P] = P(
////    queryPrimary ~/ querySetClause.?
////  )
////
////  def querySetClause[$:P] = P(
////    Keywords("INTERSECT","UNION","EXCEPT") ~/ Keywords("DISTINCT","ALL").? ~/ queryTerm
////  )
////
////  def queryPrimary[$:P] = P(Pass)
////
////  def sortItem[$: P] = P(Pass)
////
////  def Keywords[$: P](expected: String*): P[Unit] = P(
////    anyKeywords.opaque(expected.mkString(" or "))
////      .filter { kw =>
////        expected.exists {
////          _.equalsIgnoreCase(kw)
////        }
////      }
////  )
////
////  def unQuotedIdentifier[$: P]: P[String] = P(
////    !anyKeywords ~
////      (
////        (CharIn("A-Z0-9_") ~/ CharsWhileIn("A-Z0-9_@:").?).!
////          | (CharIn("0-9") ~ CharsWhileIn("A-Z0-9_@:")).!)
////  )
////
////  def quotedIdentifier[$: P]: P[String] = P(
////    ("\"" ~/ (CharPred(_ != '"') | escapeQuote).rep.! ~ "\"")
////      | ("`" ~/ (CharPred(_ != '`') | escapeQuote).rep.! ~ "`")
////  )
////
////  def identifier[$: P]: P[String] = P(unQuotedIdentifier | quotedIdentifier)
////
////  def anyKeywords[$: P]: P[String] = P(
////    StringIn(
////      "ADD",
////      "ADMIN",
////      "ALL",
////      "ANALYZE",
////      "ANY",
////      "ARRAY",
////      "ASC",
////      "AT",
////      "BERNOULLI",
////      "CALL",
////      "CALLED",
////      "CASCADE",
////      "CATALOGS",
////      "COLUMN",
////      "COLUMNS",
////      "COMMENT",
////      "COMMIT",
////      "COMMITTED",
////      "CURRENT",
////      "CURRENT_ROLE",
////      "DATA",
////      "DATE",
////      "DAY",
////      "DEFINER",
////      "DESC",
////      "DETERMINISTIC",
////      "DISTRIBUTED",
////      "EXCLUDING",
////      "EXPLAIN",
////      "EXTERNAL",
////      "FETCH",
////      "FILTER",
////      "FIRST",
////      "FOLLOWING",
////      "FORMAT",
////      "FUNCTION",
////      "FUNCTIONS",
////      "GRANT",
////      "GRANTED",
////      "GRANTS",
////      "GRAPHVIZ",
////      "GROUPS",
////      "HOUR",
////      "IF",
////      "IGNORE",
////      "INCLUDING",
////      "INPUT",
////      "INTERVAL",
////      "INVOKER",
////      "IO",
////      "ISOLATION",
////      "JSON",
////      "LANGUAGE",
////      "LAST",
////      "LATERAL",
////      "LEVEL",
////      "LIMIT",
////      "LOGICAL",
////      "MAP",
////      "MATERIALIZED",
////      "MINUTE",
////      "MONTH",
////      "NAME",
////      "NFC",
////      "NFD",
////      "NFKC",
////      "NFKD",
////      "NO",
////      "NONE",
////      "NULLIF",
////      "NULLS",
////      "OF",
////      "OFFSET",
////      "ONLY",
////      "OPTION",
////      "ORDINALITY",
////      "OUTPUT",
////      "OVER",
////      "PARTITION",
////      "PARTITIONS",
////      "POSITION",
////      "PRECEDING",
////      "PRIVILEGES",
////      "PROPERTIES",
////      "RANGE",
////      "READ",
////      "REFRESH",
////      "RENAME",
////      "REPEATABLE",
////      "REPLACE",
////      "RESET",
////      "RESPECT",
////      "RESTRICT",
////      "RETURN",
////      "RETURNS",
////      "REVOKE",
////      "ROLE",
////      "ROLES",
////      "ROLLBACK",
////      "ROW",
////      "ROWS",
////      "SCHEMA",
////      "SCHEMAS",
////      "SECOND",
////      "SECURITY",
////      "SERIALIZABLE",
////      "SESSION",
////      "SET",
////      "SETS",
////      "SQL",
////      "SHOW",
////      "SOME",
////      "START",
////      "STATS",
////      "SUBSTRING",
////      "SYSTEM",
////      "SYSTEM_TIME",
////      "SYSTEM_VERSION",
////      "TABLES",
////      "TABLESAMPLE",
////      "TEMPORARY",
////      "TEXT",
////      "TIME",
////      "TIMESTAMP",
////      "TO",
////      "TRANSACTION",
////      "TRUNCATE",
////      "TRY_CAST",
////      "TYPE",
////      "UNBOUNDED",
////      "UNCOMMITTED",
////      "USE",
////      "USER",
////      "VALIDATE",
////      "VERBOSE",
////      "VERSION",
////      "VIEW",
////      "WORK",
////      "WRITE",
////      "YEAR",
////      "ZONE"
////    ).! ~ !CharIn("A-Z0-9_@:")
////  )
////
////  def digits[$: P]: P[Unit] = P(CharsWhileIn("0-9"))
////
////  def arithmeticUnary[$: P]: P[Unit] = P("-" | "+")
////
////  def escapeQuote[$: P]: P[String] = P(
////    ("''").!.map {
////      _.replaceAll("''", "'")
////    } | ("\"\"").!.map {
////      _.replaceAll("\"\"", "\"")
////    } | ("``").!.map {
////      _.replaceAll("``", "`")
////    }
////  )
////
////
////  def integerLiteral[$: P]: P[String] = P((arithmeticUnary.? ~ digits).! ~ !("."))
////
////  def decimalLiteral[$: P]: P[String] = P((arithmeticUnary.? ~ digits ~ ("." ~ digits).? ~ ("E" ~ arithmeticUnary.? ~ digits).?).!)
////
////  def stringLiteral[$: P]: P[String] = P("'" ~/ (CharsWhile(_ != '\'') | escapeQuote).rep.! ~ "'")
//}
