package info.victorchu.octopus.sql.parser.parsec.dialect

import fastparse.*
import info.victorchu.octopus.sql.parser.parsec.SqlWhitespace.*
import info.victorchu.octopus.sql.parser.parsec.dialect.PrestoSqlParser.Keywords

object PrestoSqlParser {

  def singleStatement[$: P] = P(
    statement ~ End
  )

  def statement[$: P] = P(
    queryStatement
  )

  def queryStatement[$: P] = P(
    withClause.? ~ queryNoWith
  )

  def withClause[$: P] = P(
    Keywords("WITH") ~/ Keywords("RECURSIVE").? ~ namedQuery ~ P("," ~ namedQuery).rep
  )

  def namedQuery[$: P] = Pass

  def queryNoWith[$:P] = Pass

  def Keywords[$: P](expected: String*): P[Unit] =
    anyKeywords.opaque(expected.mkString(" or "))
      .filter { kw =>
        expected.exists {
          _.equalsIgnoreCase(kw)
        }
      }

  def unQuotedIdentifier[$: P]: P[String] =
    !anyKeywords ~
      (
        (CharIn("A-Z0-9_") ~ CharsWhileIn("A-Z0-9_@:").?).!
          | (CharIn("0-9") ~ CharsWhileIn("A-Z0-9_@:").rep(1)).!)

  def quotedIdentifier[$: P]: P[String] =
    ("\"" ~/ (CharPred(_ != '"') | escapeQuote).rep(1).! ~ "\"")
      | ("`" ~/ (CharPred(_ != '`') | escapeQuote).rep(1).! ~ "`")

  def identifier[$: P]: P[String] = P(unQuotedIdentifier | quotedIdentifier)

  def anyKeywords[$: P]: P[String] =
    StringIn(
      "ADD",
      "ADMIN",
      "ALL",
      "ANALYZE",
      "ANY",
      "ARRAY",
      "ASC",
      "AT",
      "BERNOULLI",
      "CALL",
      "CALLED",
      "CASCADE",
      "CATALOGS",
      "COLUMN",
      "COLUMNS",
      "COMMENT",
      "COMMIT",
      "COMMITTED",
      "CURRENT",
      "CURRENT_ROLE",
      "DATA",
      "DATE",
      "DAY",
      "DEFINER",
      "DESC",
      "DETERMINISTIC",
      "DISTRIBUTED",
      "EXCLUDING",
      "EXPLAIN",
      "EXTERNAL",
      "FETCH",
      "FILTER",
      "FIRST",
      "FOLLOWING",
      "FORMAT",
      "FUNCTION",
      "FUNCTIONS",
      "GRANT",
      "GRANTED",
      "GRANTS",
      "GRAPHVIZ",
      "GROUPS",
      "HOUR",
      "IF",
      "IGNORE",
      "INCLUDING",
      "INPUT",
      "INTERVAL",
      "INVOKER",
      "IO",
      "ISOLATION",
      "JSON",
      "LANGUAGE",
      "LAST",
      "LATERAL",
      "LEVEL",
      "LIMIT",
      "LOGICAL",
      "MAP",
      "MATERIALIZED",
      "MINUTE",
      "MONTH",
      "NAME",
      "NFC",
      "NFD",
      "NFKC",
      "NFKD",
      "NO",
      "NONE",
      "NULLIF",
      "NULLS",
      "OF",
      "OFFSET",
      "ONLY",
      "OPTION",
      "ORDINALITY",
      "OUTPUT",
      "OVER",
      "PARTITION",
      "PARTITIONS",
      "POSITION",
      "PRECEDING",
      "PRIVILEGES",
      "PROPERTIES",
      "RANGE",
      "READ",
      "REFRESH",
      "RENAME",
      "REPEATABLE",
      "REPLACE",
      "RESET",
      "RESPECT",
      "RESTRICT",
      "RETURN",
      "RETURNS",
      "REVOKE",
      "ROLE",
      "ROLES",
      "ROLLBACK",
      "ROW",
      "ROWS",
      "SCHEMA",
      "SCHEMAS",
      "SECOND",
      "SECURITY",
      "SERIALIZABLE",
      "SESSION",
      "SET",
      "SETS",
      "SQL",
      "SHOW",
      "SOME",
      "START",
      "STATS",
      "SUBSTRING",
      "SYSTEM",
      "SYSTEM_TIME",
      "SYSTEM_VERSION",
      "TABLES",
      "TABLESAMPLE",
      "TEMPORARY",
      "TEXT",
      "TIME",
      "TIMESTAMP",
      "TO",
      "TRANSACTION",
      "TRUNCATE",
      "TRY_CAST",
      "TYPE",
      "UNBOUNDED",
      "UNCOMMITTED",
      "USE",
      "USER",
      "VALIDATE",
      "VERBOSE",
      "VERSION",
      "VIEW",
      "WORK",
      "WRITE",
      "YEAR",
      "ZONE"
    ).! ~ !CharIn("A-Z0-9_@:")

  def digits[$: P]: P[Unit] = CharsWhileIn("0-9")

  def arithmeticUnary[$: P]: P[Unit] = "-" | "+"

  def escapeQuote[$: P]: P[String] =
    ("''").!.map {
      _.replaceAll("''", "'")
    } | ("\"\"").!.map {
      _.replaceAll("\"\"", "\"")
    } | ("``").!.map {
      _.replaceAll("``", "`")
    }


  def integerLiteral[$: P]: P[String] = (arithmeticUnary.? ~ digits).! ~ !(".")

  def decimalLiteral[$: P]: P[String] = (arithmeticUnary.? ~ digits ~ ("." ~ digits).? ~ ("E" ~ arithmeticUnary.? ~ digits).?).!

  def stringLiteral[$: P]: P[String] = "'" ~ (CharsWhile(_ != '\'') | escapeQuote).rep.! ~ "'"
}
