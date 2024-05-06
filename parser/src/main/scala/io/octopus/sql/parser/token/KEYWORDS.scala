package io.octopus.sql.parser.token

import enumeratum.*

sealed trait KEYWORD(override val entryName: String) extends EnumEntry

object KEYWORDS extends Enum[KEYWORD] {
  val values: IndexedSeq[KEYWORD] = findValues

  case object ACCESSIBLE extends KEYWORD("ACCESSIBLE")

  case object ACCOUNT extends KEYWORD("ACCOUNT")

  case object ACTION extends KEYWORD("ACTION")

  case object ACTIVE extends KEYWORD("ACTIVE")

  case object ADD extends KEYWORD("ADD")

  case object ADMIN extends KEYWORD("ADMIN")

  case object AFTER extends KEYWORD("AFTER")

  case object AGAINST extends KEYWORD("AGAINST")

  case object AGGREGATE extends KEYWORD("AGGREGATE")

  case object ALGORITHM extends KEYWORD("ALGORITHM")

  case object ALL extends KEYWORD("ALL")

  case object ALTER extends KEYWORD("ALTER")

  case object ALWAYS extends KEYWORD("ALWAYS")

  case object ANALYSE extends KEYWORD("ANALYSE")

  case object ANALYZE extends KEYWORD("ANALYZE")

  case object AND extends KEYWORD("AND")

  case object ANY extends KEYWORD("ANY")

  case object ARRAY extends KEYWORD("ARRAY")

  case object AS extends KEYWORD("AS")

  case object ASC extends KEYWORD("ASC")

  case object ASCII extends KEYWORD("ASCII")

  case object ASENSITIVE extends KEYWORD("ASENSITIVE")

  case object AT extends KEYWORD("AT")

  case object ATTRIBUTE extends KEYWORD("ATTRIBUTE")

  case object AUTHENTICATION extends KEYWORD("AUTHENTICATION")

  case object AUTOEXTEND_SIZE extends KEYWORD("AUTOEXTEND_SIZE")

  case object AUTO_INCREMENT extends KEYWORD("AUTO_INCREMENT")

  case object AVG extends KEYWORD("AVG")

  case object AVG_ROW_LENGTH extends KEYWORD("AVG_ROW_LENGTH")

  case object BACKUP extends KEYWORD("BACKUP")

  case object BEFORE extends KEYWORD("BEFORE")

  case object BEGIN extends KEYWORD("BEGIN")

  case object BERNOULLI extends KEYWORD("BERNOULLI")

  case object BETWEEN extends KEYWORD("BETWEEN")

  case object BIGINT extends KEYWORD("BIGINT")

  case object BINARY extends KEYWORD("BINARY")

  case object BINLOG extends KEYWORD("BINLOG")

  case object BIT extends KEYWORD("BIT")

  case object BLOB extends KEYWORD("BLOB")

  case object BLOCK extends KEYWORD("BLOCK")

  case object BOOL extends KEYWORD("BOOL")

  case object BOOLEAN extends KEYWORD("BOOLEAN")

  case object BOTH extends KEYWORD("BOTH")

  case object BTREE extends KEYWORD("BTREE")

  case object BUCKETS extends KEYWORD("BUCKETS")

  case object BULK extends KEYWORD("BULK")

  case object BY extends KEYWORD("BY")

  case object BYTE extends KEYWORD("BYTE")

  case object CACHE extends KEYWORD("CACHE")

  case object CALL extends KEYWORD("CALL")

  case object CALLED extends KEYWORD("CALLED")

  case object CASCADE extends KEYWORD("CASCADE")

  case object CASCADED extends KEYWORD("CASCADED")

  case object CASE extends KEYWORD("CASE")

  case object CAST extends KEYWORD("CAST")

  case object CATALOGS extends KEYWORD("CATALOGS")

  case object CATALOG_NAME extends KEYWORD("CATALOG_NAME")

  case object CHAIN extends KEYWORD("CHAIN")

  case object CHALLENGE_RESPONSE extends KEYWORD("CHALLENGE_RESPONSE")

  case object CHANGE extends KEYWORD("CHANGE")

  case object CHANGED extends KEYWORD("CHANGED")

  case object CHANNEL extends KEYWORD("CHANNEL")

  case object CHAR extends KEYWORD("CHAR")

  case object CHARACTER extends KEYWORD("CHARACTER")

  case object CHARSET extends KEYWORD("CHARSET")

  case object CHECK extends KEYWORD("CHECK")

  case object CHECKSUM extends KEYWORD("CHECKSUM")

  case object CIPHER extends KEYWORD("CIPHER")

  case object CLASS_ORIGIN extends KEYWORD("CLASS_ORIGIN")

  case object CLIENT extends KEYWORD("CLIENT")

  case object CLONE extends KEYWORD("CLONE")

  case object CLOSE extends KEYWORD("CLOSE")

  case object COALESCE extends KEYWORD("COALESCE")

  case object CODE extends KEYWORD("CODE")

  case object COLLATE extends KEYWORD("COLLATE")

  case object COLLATION extends KEYWORD("COLLATION")

  case object COLUMN extends KEYWORD("COLUMN")

  case object COLUMNS extends KEYWORD("COLUMNS")

  case object COLUMN_FORMAT extends KEYWORD("COLUMN_FORMAT")

  case object COLUMN_NAME extends KEYWORD("COLUMN_NAME")

  case object COMMENT extends KEYWORD("COMMENT")

  case object COMMIT extends KEYWORD("COMMIT")

  case object COMMITTED extends KEYWORD("COMMITTED")

  case object COMPACT extends KEYWORD("COMPACT")

  case object COMPLETION extends KEYWORD("COMPLETION")

  case object COMPONENT extends KEYWORD("COMPONENT")

  case object COMPRESSED extends KEYWORD("COMPRESSED")

  case object COMPRESSION extends KEYWORD("COMPRESSION")

  case object CONCURRENT extends KEYWORD("CONCURRENT")

  case object CONDITION extends KEYWORD("CONDITION")

  case object CONNECTION extends KEYWORD("CONNECTION")

  case object CONSISTENT extends KEYWORD("CONSISTENT")

  case object CONSTRAINT extends KEYWORD("CONSTRAINT")

  case object CONSTRAINT_CATALOG extends KEYWORD("CONSTRAINT_CATALOG")

  case object CONSTRAINT_NAME extends KEYWORD("CONSTRAINT_NAME")

  case object CONSTRAINT_SCHEMA extends KEYWORD("CONSTRAINT_SCHEMA")

  case object CONTAINS extends KEYWORD("CONTAINS")

  case object CONTEXT extends KEYWORD("CONTEXT")

  case object CONTINUE extends KEYWORD("CONTINUE")

  case object CONVERT extends KEYWORD("CONVERT")

  case object CPU extends KEYWORD("CPU")

  case object CREATE extends KEYWORD("CREATE")

  case object CROSS extends KEYWORD("CROSS")

  case object CUBE extends KEYWORD("CUBE")

  case object CUME_DIST extends KEYWORD("CUME_DIST")

  case object CURRENT extends KEYWORD("CURRENT")

  case object CURRENT_DATE extends KEYWORD("CURRENT_DATE")

  case object CURRENT_ROLE extends KEYWORD("CURRENT_ROLE")

  case object CURRENT_TIME extends KEYWORD("CURRENT_TIME")

  case object CURRENT_TIMESTAMP extends KEYWORD("CURRENT_TIMESTAMP")

  case object CURRENT_USER extends KEYWORD("CURRENT_USER")

  case object CURSOR extends KEYWORD("CURSOR")

  case object CURSOR_NAME extends KEYWORD("CURSOR_NAME")

  case object DATA extends KEYWORD("DATA")

  case object DATABASE extends KEYWORD("DATABASE")

  case object DATABASES extends KEYWORD("DATABASES")

  case object DATAFILE extends KEYWORD("DATAFILE")

  case object DATE extends KEYWORD("DATE")

  case object DATETIME extends KEYWORD("DATETIME")

  case object DAY extends KEYWORD("DAY")

  case object DAY_HOUR extends KEYWORD("DAY_HOUR")

  case object DAY_MICROSECOND extends KEYWORD("DAY_MICROSECOND")

  case object DAY_MINUTE extends KEYWORD("DAY_MINUTE")

  case object DAY_SECOND extends KEYWORD("DAY_SECOND")

  case object DEALLOCATE extends KEYWORD("DEALLOCATE")

  case object DEC extends KEYWORD("DEC")

  case object DECIMAL extends KEYWORD("DECIMAL")

  case object DECLARE extends KEYWORD("DECLARE")

  case object DEFAULT extends KEYWORD("DEFAULT")

  case object DEFAULT_AUTH extends KEYWORD("DEFAULT_AUTH")

  case object DEFINER extends KEYWORD("DEFINER")

  case object DEFINITION extends KEYWORD("DEFINITION")

  case object DELAYED extends KEYWORD("DELAYED")

  case object DELAY_KEY_WRITE extends KEYWORD("DELAY_KEY_WRITE")

  case object DELETE extends KEYWORD("DELETE")

  case object DENSE_RANK extends KEYWORD("DENSE_RANK")

  case object DESC extends KEYWORD("DESC")

  case object DESCRIBE extends KEYWORD("DESCRIBE")

  case object DESCRIPTION extends KEYWORD("DESCRIPTION")

  case object DES_KEY_FILE extends KEYWORD("DES_KEY_FILE")

  case object DETERMINISTIC extends KEYWORD("DETERMINISTIC")

  case object DIAGNOSTICS extends KEYWORD("DIAGNOSTICS")

  case object DIRECTORY extends KEYWORD("DIRECTORY")

  case object DISABLE extends KEYWORD("DISABLE")

  case object DISABLED extends KEYWORD("DISABLED")

  case object DISCARD extends KEYWORD("DISCARD")

  case object DISK extends KEYWORD("DISK")

  case object DISTINCT extends KEYWORD("DISTINCT")

  case object DISTINCTROW extends KEYWORD("DISTINCTROW")

  case object DISTRIBUTED extends KEYWORD("DISTRIBUTED")

  case object DIV extends KEYWORD("DIV")

  case object DO extends KEYWORD("DO")

  case object DOUBLE extends KEYWORD("DOUBLE")

  case object DROP extends KEYWORD("DROP")

  case object DUAL extends KEYWORD("DUAL")

  case object DUMPFILE extends KEYWORD("DUMPFILE")

  case object DUPLICATE extends KEYWORD("DUPLICATE")

  case object DYNAMIC extends KEYWORD("DYNAMIC")

  case object EACH extends KEYWORD("EACH")

  case object ELSE extends KEYWORD("ELSE")

  case object ELSEIF extends KEYWORD("ELSEIF")

  case object EMPTY extends KEYWORD("EMPTY")

  case object ENABLE extends KEYWORD("ENABLE")

  case object ENABLED extends KEYWORD("ENABLED")

  case object ENCLOSED extends KEYWORD("ENCLOSED")

  case object ENCRYPTION extends KEYWORD("ENCRYPTION")

  case object END extends KEYWORD("END")

  case object ENDS extends KEYWORD("ENDS")

  case object ENFORCED extends KEYWORD("ENFORCED")

  case object ENGINE extends KEYWORD("ENGINE")

  case object ENGINES extends KEYWORD("ENGINES")

  case object ENGINE_ATTRIBUTE extends KEYWORD("ENGINE_ATTRIBUTE")

  case object ENUM extends KEYWORD("ENUM")

  case object ERROR extends KEYWORD("ERROR")

  case object ERRORS extends KEYWORD("ERRORS")

  case object ESCAPE extends KEYWORD("ESCAPE")

  case object ESCAPED extends KEYWORD("ESCAPED")

  case object EVENT extends KEYWORD("EVENT")

  case object EVENTS extends KEYWORD("EVENTS")

  case object EVERY extends KEYWORD("EVERY")

  case object EXCEPT extends KEYWORD("EXCEPT")

  case object EXCHANGE extends KEYWORD("EXCHANGE")

  case object EXCLUDE extends KEYWORD("EXCLUDE")

  case object EXCLUDING extends KEYWORD("EXCLUDING")

  case object EXECUTE extends KEYWORD("EXECUTE")

  case object EXISTS extends KEYWORD("EXISTS")

  case object EXIT extends KEYWORD("EXIT")

  case object EXPANSION extends KEYWORD("EXPANSION")

  case object EXPIRE extends KEYWORD("EXPIRE")

  case object EXPLAIN extends KEYWORD("EXPLAIN")

  case object EXPORT extends KEYWORD("EXPORT")

  case object EXTENDED extends KEYWORD("EXTENDED")

  case object EXTENT_SIZE extends KEYWORD("EXTENT_SIZE")

  case object EXTERNAL extends KEYWORD("EXTERNAL")

  case object EXTRACT extends KEYWORD("EXTRACT")

  case object FACTOR extends KEYWORD("FACTOR")

  case object FAILED_LOGIN_ATTEMPTS extends KEYWORD("FAILED_LOGIN_ATTEMPTS")

  case object FALSE extends KEYWORD("FALSE")

  case object FAST extends KEYWORD("FAST")

  case object FAULTS extends KEYWORD("FAULTS")

  case object FETCH extends KEYWORD("FETCH")

  case object FIELDS extends KEYWORD("FIELDS")

  case object FILE extends KEYWORD("FILE")

  case object FILE_BLOCK_SIZE extends KEYWORD("FILE_BLOCK_SIZE")

  case object FILTER extends KEYWORD("FILTER")

  case object FINISH extends KEYWORD("FINISH")

  case object FIRST extends KEYWORD("FIRST")

  case object FIRST_VALUE extends KEYWORD("FIRST_VALUE")

  case object FIXED extends KEYWORD("FIXED")

  case object FLOAT extends KEYWORD("FLOAT")

  case object FLOAT4 extends KEYWORD("FLOAT4")

  case object FLOAT8 extends KEYWORD("FLOAT8")

  case object FLUSH extends KEYWORD("FLUSH")

  case object FOLLOWING extends KEYWORD("FOLLOWING")

  case object FOLLOWS extends KEYWORD("FOLLOWS")

  case object FOR extends KEYWORD("FOR")

  case object FORCE extends KEYWORD("FORCE")

  case object FOREIGN extends KEYWORD("FOREIGN")

  case object FORMAT extends KEYWORD("FORMAT")

  case object FOUND extends KEYWORD("FOUND")

  case object FROM extends KEYWORD("FROM")

  case object FULL extends KEYWORD("FULL")

  case object FULLTEXT extends KEYWORD("FULLTEXT")

  case object FUNCTION extends KEYWORD("FUNCTION")

  case object FUNCTIONS extends KEYWORD("FUNCTIONS")

  case object GENERAL extends KEYWORD("GENERAL")

  case object GENERATE extends KEYWORD("GENERATE")

  case object GENERATED extends KEYWORD("GENERATED")

  case object GEOMCOLLECTION extends KEYWORD("GEOMCOLLECTION")

  case object GEOMETRY extends KEYWORD("GEOMETRY")

  case object GEOMETRYCOLLECTION extends KEYWORD("GEOMETRYCOLLECTION")

  case object GET extends KEYWORD("GET")

  case object GET_FORMAT extends KEYWORD("GET_FORMAT")

  case object GET_MASTER_PUBLIC_KEY extends KEYWORD("GET_MASTER_PUBLIC_KEY")

  case object GET_SOURCE_PUBLIC_KEY extends KEYWORD("GET_SOURCE_PUBLIC_KEY")

  case object GLOBAL extends KEYWORD("GLOBAL")

  case object GRANT extends KEYWORD("GRANT")

  case object GRANTED extends KEYWORD("GRANTED")

  case object GRANTS extends KEYWORD("GRANTS")

  case object GRAPHVIZ extends KEYWORD("GRAPHVIZ")

  case object GROUP extends KEYWORD("GROUP")

  case object GROUPING extends KEYWORD("GROUPING")

  case object GROUPS extends KEYWORD("GROUPS")

  case object GROUP_REPLICATION extends KEYWORD("GROUP_REPLICATION")

  case object GTID_ONLY extends KEYWORD("GTID_ONLY")

  case object HANDLER extends KEYWORD("HANDLER")

  case object HASH extends KEYWORD("HASH")

  case object HAVING extends KEYWORD("HAVING")

  case object HELP extends KEYWORD("HELP")

  case object HIGH_PRIORITY extends KEYWORD("HIGH_PRIORITY")

  case object HISTOGRAM extends KEYWORD("HISTOGRAM")

  case object HISTORY extends KEYWORD("HISTORY")

  case object HOST extends KEYWORD("HOST")

  case object HOSTS extends KEYWORD("HOSTS")

  case object HOUR extends KEYWORD("HOUR")

  case object HOUR_MICROSECOND extends KEYWORD("HOUR_MICROSECOND")

  case object HOUR_MINUTE extends KEYWORD("HOUR_MINUTE")

  case object HOUR_SECOND extends KEYWORD("HOUR_SECOND")

  case object IDENTIFIED extends KEYWORD("IDENTIFIED")

  case object IF extends KEYWORD("IF")

  case object IGNORE extends KEYWORD("IGNORE")

  case object IGNORE_SERVER_IDS extends KEYWORD("IGNORE_SERVER_IDS")

  case object IMPORT extends KEYWORD("IMPORT")

  case object IN extends KEYWORD("IN")

  case object INACTIVE extends KEYWORD("INACTIVE")

  case object INCLUDING extends KEYWORD("INCLUDING")

  case object INDEX extends KEYWORD("INDEX")

  case object INDEXES extends KEYWORD("INDEXES")

  case object INFILE extends KEYWORD("INFILE")

  case object INITIAL extends KEYWORD("INITIAL")

  case object INITIAL_SIZE extends KEYWORD("INITIAL_SIZE")

  case object INITIATE extends KEYWORD("INITIATE")

  case object INNER extends KEYWORD("INNER")

  case object INOUT extends KEYWORD("INOUT")

  case object INPUT extends KEYWORD("INPUT")

  case object INSENSITIVE extends KEYWORD("INSENSITIVE")

  case object INSERT extends KEYWORD("INSERT")

  case object INSERT_METHOD extends KEYWORD("INSERT_METHOD")

  case object INSTALL extends KEYWORD("INSTALL")

  case object INSTANCE extends KEYWORD("INSTANCE")

  case object INT extends KEYWORD("INT")

  case object INT1 extends KEYWORD("INT1")

  case object INT2 extends KEYWORD("INT2")

  case object INT3 extends KEYWORD("INT3")

  case object INT4 extends KEYWORD("INT4")

  case object INT8 extends KEYWORD("INT8")

  case object INTEGER extends KEYWORD("INTEGER")

  case object INTERSECT extends KEYWORD("INTERSECT")

  case object INTERVAL extends KEYWORD("INTERVAL")

  case object INTO extends KEYWORD("INTO")

  case object INVISIBLE extends KEYWORD("INVISIBLE")

  case object INVOKER extends KEYWORD("INVOKER")

  case object IO extends KEYWORD("IO")

  case object IO_AFTER_GTIDS extends KEYWORD("IO_AFTER_GTIDS")

  case object IO_BEFORE_GTIDS extends KEYWORD("IO_BEFORE_GTIDS")

  case object IO_THREAD extends KEYWORD("IO_THREAD")

  case object IPC extends KEYWORD("IPC")

  case object IS extends KEYWORD("IS")

  case object ISOLATION extends KEYWORD("ISOLATION")

  case object ISSUER extends KEYWORD("ISSUER")

  case object ITERATE extends KEYWORD("ITERATE")

  case object JOIN extends KEYWORD("JOIN")

  case object JSON extends KEYWORD("JSON")

  case object JSON_TABLE extends KEYWORD("JSON_TABLE")

  case object JSON_VALUE extends KEYWORD("JSON_VALUE")

  case object KEY extends KEYWORD("KEY")

  case object KEYRING extends KEYWORD("KEYRING")

  case object KEYS extends KEYWORD("KEYS")

  case object KEY_BLOCK_SIZE extends KEYWORD("KEY_BLOCK_SIZE")

  case object KILL extends KEYWORD("KILL")

  case object LAG extends KEYWORD("LAG")

  case object LANGUAGE extends KEYWORD("LANGUAGE")

  case object LAST extends KEYWORD("LAST")

  case object LAST_VALUE extends KEYWORD("LAST_VALUE")

  case object LATERAL extends KEYWORD("LATERAL")

  case object LEAD extends KEYWORD("LEAD")

  case object LEADING extends KEYWORD("LEADING")

  case object LEAVE extends KEYWORD("LEAVE")

  case object LEAVES extends KEYWORD("LEAVES")

  case object LEFT extends KEYWORD("LEFT")

  case object LESS extends KEYWORD("LESS")

  case object LEVEL extends KEYWORD("LEVEL")

  case object LIKE extends KEYWORD("LIKE")

  case object LIMIT extends KEYWORD("LIMIT")

  case object LINEAR extends KEYWORD("LINEAR")

  case object LINES extends KEYWORD("LINES")

  case object LINESTRING extends KEYWORD("LINESTRING")

  case object LIST extends KEYWORD("LIST")

  case object LOAD extends KEYWORD("LOAD")

  case object LOCAL extends KEYWORD("LOCAL")

  case object LOCALTIME extends KEYWORD("LOCALTIME")

  case object LOCALTIMESTAMP extends KEYWORD("LOCALTIMESTAMP")

  case object LOCK extends KEYWORD("LOCK")

  case object LOCKED extends KEYWORD("LOCKED")

  case object LOCKS extends KEYWORD("LOCKS")

  case object LOGFILE extends KEYWORD("LOGFILE")

  case object LOGICAL extends KEYWORD("LOGICAL")

  case object LOGS extends KEYWORD("LOGS")

  case object LONG extends KEYWORD("LONG")

  case object LONGBLOB extends KEYWORD("LONGBLOB")

  case object LONGTEXT extends KEYWORD("LONGTEXT")

  case object LOOP extends KEYWORD("LOOP")

  case object LOW_PRIORITY extends KEYWORD("LOW_PRIORITY")

  case object MAP extends KEYWORD("MAP")

  case object MASTER extends KEYWORD("MASTER")

  case object MASTER_AUTO_POSITION extends KEYWORD("MASTER_AUTO_POSITION")

  case object MASTER_BIND extends KEYWORD("MASTER_BIND")

  case object MASTER_COMPRESSION_ALGORITHMS extends KEYWORD("MASTER_COMPRESSION_ALGORITHMS")

  case object MASTER_CONNECT_RETRY extends KEYWORD("MASTER_CONNECT_RETRY")

  case object MASTER_DELAY extends KEYWORD("MASTER_DELAY")

  case object MASTER_HEARTBEAT_PERIOD extends KEYWORD("MASTER_HEARTBEAT_PERIOD")

  case object MASTER_HOST extends KEYWORD("MASTER_HOST")

  case object MASTER_LOG_FILE extends KEYWORD("MASTER_LOG_FILE")

  case object MASTER_LOG_POS extends KEYWORD("MASTER_LOG_POS")

  case object MASTER_PASSWORD extends KEYWORD("MASTER_PASSWORD")

  case object MASTER_PORT extends KEYWORD("MASTER_PORT")

  case object MASTER_PUBLIC_KEY_PATH extends KEYWORD("MASTER_PUBLIC_KEY_PATH")

  case object MASTER_RETRY_COUNT extends KEYWORD("MASTER_RETRY_COUNT")

  case object MASTER_SERVER_ID extends KEYWORD("MASTER_SERVER_ID")

  case object MASTER_SSL extends KEYWORD("MASTER_SSL")

  case object MASTER_SSL_CA extends KEYWORD("MASTER_SSL_CA")

  case object MASTER_SSL_CAPATH extends KEYWORD("MASTER_SSL_CAPATH")

  case object MASTER_SSL_CERT extends KEYWORD("MASTER_SSL_CERT")

  case object MASTER_SSL_CIPHER extends KEYWORD("MASTER_SSL_CIPHER")

  case object MASTER_SSL_CRL extends KEYWORD("MASTER_SSL_CRL")

  case object MASTER_SSL_CRLPATH extends KEYWORD("MASTER_SSL_CRLPATH")

  case object MASTER_SSL_KEY extends KEYWORD("MASTER_SSL_KEY")

  case object MASTER_SSL_VERIFY_SERVER_CERT extends KEYWORD("MASTER_SSL_VERIFY_SERVER_CERT")

  case object MASTER_TLS_CIPHERSUITES extends KEYWORD("MASTER_TLS_CIPHERSUITES")

  case object MASTER_TLS_VERSION extends KEYWORD("MASTER_TLS_VERSION")

  case object MASTER_USER extends KEYWORD("MASTER_USER")

  case object MASTER_ZSTD_COMPRESSION_LEVEL extends KEYWORD("MASTER_ZSTD_COMPRESSION_LEVEL")

  case object MATCH extends KEYWORD("MATCH")

  case object MATERIALIZED extends KEYWORD("MATERIALIZED")

  case object MAXVALUE extends KEYWORD("MAXVALUE")

  case object MAX_CONNECTIONS_PER_HOUR extends KEYWORD("MAX_CONNECTIONS_PER_HOUR")

  case object MAX_QUERIES_PER_HOUR extends KEYWORD("MAX_QUERIES_PER_HOUR")

  case object MAX_ROWS extends KEYWORD("MAX_ROWS")

  case object MAX_SIZE extends KEYWORD("MAX_SIZE")

  case object MAX_UPDATES_PER_HOUR extends KEYWORD("MAX_UPDATES_PER_HOUR")

  case object MAX_USER_CONNECTIONS extends KEYWORD("MAX_USER_CONNECTIONS")

  case object MEDIUM extends KEYWORD("MEDIUM")

  case object MEDIUMBLOB extends KEYWORD("MEDIUMBLOB")

  case object MEDIUMINT extends KEYWORD("MEDIUMINT")

  case object MEDIUMTEXT extends KEYWORD("MEDIUMTEXT")

  case object MEMBER extends KEYWORD("MEMBER")

  case object MEMORY extends KEYWORD("MEMORY")

  case object MERGE extends KEYWORD("MERGE")

  case object MESSAGE_TEXT extends KEYWORD("MESSAGE_TEXT")

  case object MICROSECOND extends KEYWORD("MICROSECOND")

  case object MIDDLEINT extends KEYWORD("MIDDLEINT")

  case object MIGRATE extends KEYWORD("MIGRATE")

  case object MINUTE extends KEYWORD("MINUTE")

  case object MINUTE_MICROSECOND extends KEYWORD("MINUTE_MICROSECOND")

  case object MINUTE_SECOND extends KEYWORD("MINUTE_SECOND")

  case object MIN_ROWS extends KEYWORD("MIN_ROWS")

  case object MOD extends KEYWORD("MOD")

  case object MODE extends KEYWORD("MODE")

  case object MODIFIES extends KEYWORD("MODIFIES")

  case object MODIFY extends KEYWORD("MODIFY")

  case object MONTH extends KEYWORD("MONTH")

  case object MULTILINESTRING extends KEYWORD("MULTILINESTRING")

  case object MULTIPOINT extends KEYWORD("MULTIPOINT")

  case object MULTIPOLYGON extends KEYWORD("MULTIPOLYGON")

  case object MUTEX extends KEYWORD("MUTEX")

  case object MYSQL_ERRNO extends KEYWORD("MYSQL_ERRNO")

  case object NAME extends KEYWORD("NAME")

  case object NAMES extends KEYWORD("NAMES")

  case object NATIONAL extends KEYWORD("NATIONAL")

  case object NATURAL extends KEYWORD("NATURAL")

  case object NCHAR extends KEYWORD("NCHAR")

  case object NDB extends KEYWORD("NDB")

  case object NDBCLUSTER extends KEYWORD("NDBCLUSTER")

  case object NESTED extends KEYWORD("NESTED")

  case object NETWORK_NAMESPACE extends KEYWORD("NETWORK_NAMESPACE")

  case object NEVER extends KEYWORD("NEVER")

  case object NEW extends KEYWORD("NEW")

  case object NEXT extends KEYWORD("NEXT")

  case object NFC extends KEYWORD("NFC")

  case object NFD extends KEYWORD("NFD")

  case object NFKC extends KEYWORD("NFKC")

  case object NFKD extends KEYWORD("NFKD")

  case object NO extends KEYWORD("NO")

  case object NODEGROUP extends KEYWORD("NODEGROUP")

  case object NONE extends KEYWORD("NONE")

  case object NORMALIZE extends KEYWORD("NORMALIZE")

  case object NOT extends KEYWORD("NOT")

  case object NOWAIT extends KEYWORD("NOWAIT")

  case object NO_WAIT extends KEYWORD("NO_WAIT")

  case object NO_WRITE_TO_BINLOG extends KEYWORD("NO_WRITE_TO_BINLOG")

  case object NTH_VALUE extends KEYWORD("NTH_VALUE")

  case object NTILE extends KEYWORD("NTILE")

  case object NULL extends KEYWORD("NULL")

  case object NULLIF extends KEYWORD("NULLIF")

  case object NULLS extends KEYWORD("NULLS")

  case object NUMBER extends KEYWORD("NUMBER")

  case object NUMERIC extends KEYWORD("NUMERIC")

  case object NVARCHAR extends KEYWORD("NVARCHAR")

  case object OF extends KEYWORD("OF")

  case object OFF extends KEYWORD("OFF")

  case object OFFSET extends KEYWORD("OFFSET")

  case object OJ extends KEYWORD("OJ")

  case object OLD extends KEYWORD("OLD")

  case object ON extends KEYWORD("ON")

  case object ONE extends KEYWORD("ONE")

  case object ONLY extends KEYWORD("ONLY")

  case object OPEN extends KEYWORD("OPEN")

  case object OPTIMIZE extends KEYWORD("OPTIMIZE")

  case object OPTIMIZER_COSTS extends KEYWORD("OPTIMIZER_COSTS")

  case object OPTION extends KEYWORD("OPTION")

  case object OPTIONAL extends KEYWORD("OPTIONAL")

  case object OPTIONALLY extends KEYWORD("OPTIONALLY")

  case object OPTIONS extends KEYWORD("OPTIONS")

  case object OR extends KEYWORD("OR")

  case object ORDER extends KEYWORD("ORDER")

  case object ORDINALITY extends KEYWORD("ORDINALITY")

  case object ORGANIZATION extends KEYWORD("ORGANIZATION")

  case object OTHERS extends KEYWORD("OTHERS")

  case object OUT extends KEYWORD("OUT")

  case object OUTER extends KEYWORD("OUTER")

  case object OUTFILE extends KEYWORD("OUTFILE")

  case object OUTPUT extends KEYWORD("OUTPUT")

  case object OVER extends KEYWORD("OVER")

  case object OWNER extends KEYWORD("OWNER")

  case object PACK_KEYS extends KEYWORD("PACK_KEYS")

  case object PAGE extends KEYWORD("PAGE")

  case object PARSER extends KEYWORD("PARSER")

  case object PARTIAL extends KEYWORD("PARTIAL")

  case object PARTITION extends KEYWORD("PARTITION")

  case object PARTITIONING extends KEYWORD("PARTITIONING")

  case object PARTITIONS extends KEYWORD("PARTITIONS")

  case object PASSWORD extends KEYWORD("PASSWORD")

  case object PASSWORD_LOCK_TIME extends KEYWORD("PASSWORD_LOCK_TIME")

  case object PATH extends KEYWORD("PATH")

  case object PERCENT_RANK extends KEYWORD("PERCENT_RANK")

  case object PERSIST extends KEYWORD("PERSIST")

  case object PERSIST_ONLY extends KEYWORD("PERSIST_ONLY")

  case object PHASE extends KEYWORD("PHASE")

  case object PLUGIN extends KEYWORD("PLUGIN")

  case object PLUGINS extends KEYWORD("PLUGINS")

  case object PLUGIN_DIR extends KEYWORD("PLUGIN_DIR")

  case object POINT extends KEYWORD("POINT")

  case object POLYGON extends KEYWORD("POLYGON")

  case object PORT extends KEYWORD("PORT")

  case object POSITION extends KEYWORD("POSITION")

  case object PRECEDES extends KEYWORD("PRECEDES")

  case object PRECEDING extends KEYWORD("PRECEDING")

  case object PRECISION extends KEYWORD("PRECISION")

  case object PREPARE extends KEYWORD("PREPARE")

  case object PRESERVE extends KEYWORD("PRESERVE")

  case object PREV extends KEYWORD("PREV")

  case object PRIMARY extends KEYWORD("PRIMARY")

  case object PRIVILEGES extends KEYWORD("PRIVILEGES")

  case object PRIVILEGE_CHECKS_USER extends KEYWORD("PRIVILEGE_CHECKS_USER")

  case object PROCEDURE extends KEYWORD("PROCEDURE")

  case object PROCESS extends KEYWORD("PROCESS")

  case object PROCESSLIST extends KEYWORD("PROCESSLIST")

  case object PROFILE extends KEYWORD("PROFILE")

  case object PROFILES extends KEYWORD("PROFILES")

  case object PROPERTIES extends KEYWORD("PROPERTIES")

  case object PROXY extends KEYWORD("PROXY")

  case object PURGE extends KEYWORD("PURGE")

  case object QUARTER extends KEYWORD("QUARTER")

  case object QUERY extends KEYWORD("QUERY")

  case object QUICK extends KEYWORD("QUICK")

  case object RANDOM extends KEYWORD("RANDOM")

  case object RANGE extends KEYWORD("RANGE")

  case object RANK extends KEYWORD("RANK")

  case object READ extends KEYWORD("READ")

  case object READS extends KEYWORD("READS")

  case object READ_ONLY extends KEYWORD("READ_ONLY")

  case object READ_WRITE extends KEYWORD("READ_WRITE")

  case object REAL extends KEYWORD("REAL")

  case object REBUILD extends KEYWORD("REBUILD")

  case object RECOVER extends KEYWORD("RECOVER")

  case object RECURSIVE extends KEYWORD("RECURSIVE")

  case object REDOFILE extends KEYWORD("REDOFILE")

  case object REDO_BUFFER_SIZE extends KEYWORD("REDO_BUFFER_SIZE")

  case object REDUNDANT extends KEYWORD("REDUNDANT")

  case object REFERENCE extends KEYWORD("REFERENCE")

  case object REFERENCES extends KEYWORD("REFERENCES")

  case object REFRESH extends KEYWORD("REFRESH")

  case object REGEXP extends KEYWORD("REGEXP")

  case object REGISTRATION extends KEYWORD("REGISTRATION")

  case object RELAY extends KEYWORD("RELAY")

  case object RELAYLOG extends KEYWORD("RELAYLOG")

  case object RELAY_LOG_FILE extends KEYWORD("RELAY_LOG_FILE")

  case object RELAY_LOG_POS extends KEYWORD("RELAY_LOG_POS")

  case object RELAY_THREAD extends KEYWORD("RELAY_THREAD")

  case object RELEASE extends KEYWORD("RELEASE")

  case object RELOAD extends KEYWORD("RELOAD")

  case object RELY extends KEYWORD("RELY")

  case object REMOTE extends KEYWORD("REMOTE")

  case object REMOVE extends KEYWORD("REMOVE")

  case object RENAME extends KEYWORD("RENAME")

  case object REORGANIZE extends KEYWORD("REORGANIZE")

  case object REPAIR extends KEYWORD("REPAIR")

  case object REPEAT extends KEYWORD("REPEAT")

  case object REPEATABLE extends KEYWORD("REPEATABLE")

  case object REPLACE extends KEYWORD("REPLACE")

  case object REPLICA extends KEYWORD("REPLICA")

  case object REPLICAS extends KEYWORD("REPLICAS")

  case object REPLICATE_DO_DB extends KEYWORD("REPLICATE_DO_DB")

  case object REPLICATE_DO_TABLE extends KEYWORD("REPLICATE_DO_TABLE")

  case object REPLICATE_IGNORE_DB extends KEYWORD("REPLICATE_IGNORE_DB")

  case object REPLICATE_IGNORE_TABLE extends KEYWORD("REPLICATE_IGNORE_TABLE")

  case object REPLICATE_REWRITE_DB extends KEYWORD("REPLICATE_REWRITE_DB")

  case object REPLICATE_WILD_DO_TABLE extends KEYWORD("REPLICATE_WILD_DO_TABLE")

  case object REPLICATE_WILD_IGNORE_TABLE extends KEYWORD("REPLICATE_WILD_IGNORE_TABLE")

  case object REPLICATION extends KEYWORD("REPLICATION")

  case object REQUIRE extends KEYWORD("REQUIRE")

  case object REQUIRE_ROW_FORMAT extends KEYWORD("REQUIRE_ROW_FORMAT")

  case object RESET extends KEYWORD("RESET")

  case object RESIGNAL extends KEYWORD("RESIGNAL")

  case object RESOURCE extends KEYWORD("RESOURCE")

  case object RESPECT extends KEYWORD("RESPECT")

  case object RESTART extends KEYWORD("RESTART")

  case object RESTORE extends KEYWORD("RESTORE")

  case object RESTRICT extends KEYWORD("RESTRICT")

  case object RESUME extends KEYWORD("RESUME")

  case object RETAIN extends KEYWORD("RETAIN")

  case object RETURN extends KEYWORD("RETURN")

  case object RETURNED_SQLSTATE extends KEYWORD("RETURNED_SQLSTATE")

  case object RETURNING extends KEYWORD("RETURNING")

  case object RETURNS extends KEYWORD("RETURNS")

  case object REUSE extends KEYWORD("REUSE")

  case object REVERSE extends KEYWORD("REVERSE")

  case object REVOKE extends KEYWORD("REVOKE")

  case object RIGHT extends KEYWORD("RIGHT")

  case object RLIKE extends KEYWORD("RLIKE")

  case object ROLE extends KEYWORD("ROLE")

  case object ROLES extends KEYWORD("ROLES")

  case object ROLLBACK extends KEYWORD("ROLLBACK")

  case object ROLLUP extends KEYWORD("ROLLUP")

  case object ROTATE extends KEYWORD("ROTATE")

  case object ROUTINE extends KEYWORD("ROUTINE")

  case object ROW extends KEYWORD("ROW")

  case object ROWS extends KEYWORD("ROWS")

  case object ROW_COUNT extends KEYWORD("ROW_COUNT")

  case object ROW_FORMAT extends KEYWORD("ROW_FORMAT")

  case object ROW_NUMBER extends KEYWORD("ROW_NUMBER")

  case object RTREE extends KEYWORD("RTREE")

  case object SAVEPOINT extends KEYWORD("SAVEPOINT")

  case object SCHEDULE extends KEYWORD("SCHEDULE")

  case object SCHEMA extends KEYWORD("SCHEMA")

  case object SCHEMAS extends KEYWORD("SCHEMAS")

  case object SCHEMA_NAME extends KEYWORD("SCHEMA_NAME")

  case object SECOND extends KEYWORD("SECOND")

  case object SECONDARY extends KEYWORD("SECONDARY")

  case object SECONDARY_ENGINE extends KEYWORD("SECONDARY_ENGINE")

  case object SECONDARY_ENGINE_ATTRIBUTE extends KEYWORD("SECONDARY_ENGINE_ATTRIBUTE")

  case object SECONDARY_LOAD extends KEYWORD("SECONDARY_LOAD")

  case object SECONDARY_UNLOAD extends KEYWORD("SECONDARY_UNLOAD")

  case object SECOND_MICROSECOND extends KEYWORD("SECOND_MICROSECOND")

  case object SECURITY extends KEYWORD("SECURITY")

  case object SELECT extends KEYWORD("SELECT")

  case object SENSITIVE extends KEYWORD("SENSITIVE")

  case object SEPARATOR extends KEYWORD("SEPARATOR")

  case object SERIAL extends KEYWORD("SERIAL")

  case object SERIALIZABLE extends KEYWORD("SERIALIZABLE")

  case object SERVER extends KEYWORD("SERVER")

  case object SESSION extends KEYWORD("SESSION")

  case object SET extends KEYWORD("SET")

  case object SETS extends KEYWORD("SETS")

  case object SHARE extends KEYWORD("SHARE")

  case object SHOW extends KEYWORD("SHOW")

  case object SHUTDOWN extends KEYWORD("SHUTDOWN")

  case object SIGNAL extends KEYWORD("SIGNAL")

  case object SIGNED extends KEYWORD("SIGNED")

  case object SIMPLE extends KEYWORD("SIMPLE")

  case object SKIP extends KEYWORD("SKIP")

  case object SLAVE extends KEYWORD("SLAVE")

  case object SLOW extends KEYWORD("SLOW")

  case object SMALLINT extends KEYWORD("SMALLINT")

  case object SNAPSHOT extends KEYWORD("SNAPSHOT")

  case object SOCKET extends KEYWORD("SOCKET")

  case object SOME extends KEYWORD("SOME")

  case object SONAME extends KEYWORD("SONAME")

  case object SOUNDS extends KEYWORD("SOUNDS")

  case object SOURCE extends KEYWORD("SOURCE")

  case object SOURCE_AUTO_POSITION extends KEYWORD("SOURCE_AUTO_POSITION")

  case object SOURCE_BIND extends KEYWORD("SOURCE_BIND")

  case object SOURCE_COMPRESSION_ALGORITHMS extends KEYWORD("SOURCE_COMPRESSION_ALGORITHMS")

  case object SOURCE_CONNECT_RETRY extends KEYWORD("SOURCE_CONNECT_RETRY")

  case object SOURCE_DELAY extends KEYWORD("SOURCE_DELAY")

  case object SOURCE_HEARTBEAT_PERIOD extends KEYWORD("SOURCE_HEARTBEAT_PERIOD")

  case object SOURCE_HOST extends KEYWORD("SOURCE_HOST")

  case object SOURCE_LOG_FILE extends KEYWORD("SOURCE_LOG_FILE")

  case object SOURCE_LOG_POS extends KEYWORD("SOURCE_LOG_POS")

  case object SOURCE_PASSWORD extends KEYWORD("SOURCE_PASSWORD")

  case object SOURCE_PORT extends KEYWORD("SOURCE_PORT")

  case object SOURCE_PUBLIC_KEY_PATH extends KEYWORD("SOURCE_PUBLIC_KEY_PATH")

  case object SOURCE_RETRY_COUNT extends KEYWORD("SOURCE_RETRY_COUNT")

  case object SOURCE_SSL extends KEYWORD("SOURCE_SSL")

  case object SOURCE_SSL_CA extends KEYWORD("SOURCE_SSL_CA")

  case object SOURCE_SSL_CAPATH extends KEYWORD("SOURCE_SSL_CAPATH")

  case object SOURCE_SSL_CERT extends KEYWORD("SOURCE_SSL_CERT")

  case object SOURCE_SSL_CIPHER extends KEYWORD("SOURCE_SSL_CIPHER")

  case object SOURCE_SSL_CRL extends KEYWORD("SOURCE_SSL_CRL")

  case object SOURCE_SSL_CRLPATH extends KEYWORD("SOURCE_SSL_CRLPATH")

  case object SOURCE_SSL_KEY extends KEYWORD("SOURCE_SSL_KEY")

  case object SOURCE_SSL_VERIFY_SERVER_CERT extends KEYWORD("SOURCE_SSL_VERIFY_SERVER_CERT")

  case object SOURCE_TLS_CIPHERSUITES extends KEYWORD("SOURCE_TLS_CIPHERSUITES")

  case object SOURCE_TLS_VERSION extends KEYWORD("SOURCE_TLS_VERSION")

  case object SOURCE_USER extends KEYWORD("SOURCE_USER")

  case object SOURCE_ZSTD_COMPRESSION_LEVEL extends KEYWORD("SOURCE_ZSTD_COMPRESSION_LEVEL")

  case object SPATIAL extends KEYWORD("SPATIAL")

  case object SPECIFIC extends KEYWORD("SPECIFIC")

  case object SQL extends KEYWORD("SQL")

  case object SQLEXCEPTION extends KEYWORD("SQLEXCEPTION")

  case object SQLSTATE extends KEYWORD("SQLSTATE")

  case object SQLWARNING extends KEYWORD("SQLWARNING")

  case object SQL_AFTER_GTIDS extends KEYWORD("SQL_AFTER_GTIDS")

  case object SQL_AFTER_MTS_GAPS extends KEYWORD("SQL_AFTER_MTS_GAPS")

  case object SQL_BEFORE_GTIDS extends KEYWORD("SQL_BEFORE_GTIDS")

  case object SQL_BIG_RESULT extends KEYWORD("SQL_BIG_RESULT")

  case object SQL_BUFFER_RESULT extends KEYWORD("SQL_BUFFER_RESULT")

  case object SQL_CACHE extends KEYWORD("SQL_CACHE")

  case object SQL_CALC_FOUND_ROWS extends KEYWORD("SQL_CALC_FOUND_ROWS")

  case object SQL_NO_CACHE extends KEYWORD("SQL_NO_CACHE")

  case object SQL_SMALL_RESULT extends KEYWORD("SQL_SMALL_RESULT")

  case object SQL_THREAD extends KEYWORD("SQL_THREAD")

  case object SQL_TSI_DAY extends KEYWORD("SQL_TSI_DAY")

  case object SQL_TSI_HOUR extends KEYWORD("SQL_TSI_HOUR")

  case object SQL_TSI_MINUTE extends KEYWORD("SQL_TSI_MINUTE")

  case object SQL_TSI_MONTH extends KEYWORD("SQL_TSI_MONTH")

  case object SQL_TSI_QUARTER extends KEYWORD("SQL_TSI_QUARTER")

  case object SQL_TSI_SECOND extends KEYWORD("SQL_TSI_SECOND")

  case object SQL_TSI_WEEK extends KEYWORD("SQL_TSI_WEEK")

  case object SQL_TSI_YEAR extends KEYWORD("SQL_TSI_YEAR")

  case object SRID extends KEYWORD("SRID")

  case object SSL extends KEYWORD("SSL")

  case object STACKED extends KEYWORD("STACKED")

  case object START extends KEYWORD("START")

  case object STARTING extends KEYWORD("STARTING")

  case object STARTS extends KEYWORD("STARTS")

  case object STATS extends KEYWORD("STATS")

  case object STATS_AUTO_RECALC extends KEYWORD("STATS_AUTO_RECALC")

  case object STATS_PERSISTENT extends KEYWORD("STATS_PERSISTENT")

  case object STATS_SAMPLE_PAGES extends KEYWORD("STATS_SAMPLE_PAGES")

  case object STATUS extends KEYWORD("STATUS")

  case object STOP extends KEYWORD("STOP")

  case object STORAGE extends KEYWORD("STORAGE")

  case object STORED extends KEYWORD("STORED")

  case object STRAIGHT_JOIN extends KEYWORD("STRAIGHT_JOIN")

  case object STREAM extends KEYWORD("STREAM")

  case object STRING extends KEYWORD("STRING")

  case object SUBCLASS_ORIGIN extends KEYWORD("SUBCLASS_ORIGIN")

  case object SUBJECT extends KEYWORD("SUBJECT")

  case object SUBPARTITION extends KEYWORD("SUBPARTITION")

  case object SUBPARTITIONS extends KEYWORD("SUBPARTITIONS")

  case object SUBSTRING extends KEYWORD("SUBSTRING")

  case object SUPER extends KEYWORD("SUPER")

  case object SUSPEND extends KEYWORD("SUSPEND")

  case object SWAPS extends KEYWORD("SWAPS")

  case object SWITCHES extends KEYWORD("SWITCHES")

  case object SYSTEM extends KEYWORD("SYSTEM")

  case object SYSTEM_TIME extends KEYWORD("SYSTEM_TIME")

  case object SYSTEM_VERSION extends KEYWORD("SYSTEM_VERSION")

  case object TABLE extends KEYWORD("TABLE")

  case object TABLES extends KEYWORD("TABLES")

  case object TABLESAMPLE extends KEYWORD("TABLESAMPLE")

  case object TABLESPACE extends KEYWORD("TABLESPACE")

  case object TABLE_CHECKSUM extends KEYWORD("TABLE_CHECKSUM")

  case object TABLE_NAME extends KEYWORD("TABLE_NAME")

  case object TEMPORARY extends KEYWORD("TEMPORARY")

  case object TEMPTABLE extends KEYWORD("TEMPTABLE")

  case object TERMINATED extends KEYWORD("TERMINATED")

  case object TEXT extends KEYWORD("TEXT")

  case object THAN extends KEYWORD("THAN")

  case object THEN extends KEYWORD("THEN")

  case object THREAD_PRIORITY extends KEYWORD("THREAD_PRIORITY")

  case object TIES extends KEYWORD("TIES")

  case object TIME extends KEYWORD("TIME")

  case object TIMESTAMP extends KEYWORD("TIMESTAMP")

  case object TIMESTAMPADD extends KEYWORD("TIMESTAMPADD")

  case object TIMESTAMPDIFF extends KEYWORD("TIMESTAMPDIFF")

  case object TINYBLOB extends KEYWORD("TINYBLOB")

  case object TINYINT extends KEYWORD("TINYINT")

  case object TINYTEXT extends KEYWORD("TINYTEXT")

  case object TLS extends KEYWORD("TLS")

  case object TO extends KEYWORD("TO")

  case object TRAILING extends KEYWORD("TRAILING")

  case object TRANSACTION extends KEYWORD("TRANSACTION")

  case object TRIGGER extends KEYWORD("TRIGGER")

  case object TRIGGERS extends KEYWORD("TRIGGERS")

  case object TRUE extends KEYWORD("TRUE")

  case object TRUNCATE extends KEYWORD("TRUNCATE")

  case object TRY_CAST extends KEYWORD("TRY_CAST")

  case object TYPE extends KEYWORD("TYPE")

  case object TYPES extends KEYWORD("TYPES")

  case object UESCAPE extends KEYWORD("UESCAPE")

  case object UNBOUNDED extends KEYWORD("UNBOUNDED")

  case object UNCOMMITTED extends KEYWORD("UNCOMMITTED")

  case object UNDEFINED extends KEYWORD("UNDEFINED")

  case object UNDO extends KEYWORD("UNDO")

  case object UNDOFILE extends KEYWORD("UNDOFILE")

  case object UNDO_BUFFER_SIZE extends KEYWORD("UNDO_BUFFER_SIZE")

  case object UNICODE extends KEYWORD("UNICODE")

  case object UNINSTALL extends KEYWORD("UNINSTALL")

  case object UNION extends KEYWORD("UNION")

  case object UNIQUE extends KEYWORD("UNIQUE")

  case object UNKNOWN extends KEYWORD("UNKNOWN")

  case object UNLOCK extends KEYWORD("UNLOCK")

  case object UNNEST extends KEYWORD("UNNEST")

  case object UNREGISTER extends KEYWORD("UNREGISTER")

  case object UNSIGNED extends KEYWORD("UNSIGNED")

  case object UNTIL extends KEYWORD("UNTIL")

  case object UPDATE extends KEYWORD("UPDATE")

  case object UPGRADE extends KEYWORD("UPGRADE")

  case object URL extends KEYWORD("URL")

  case object USAGE extends KEYWORD("USAGE")

  case object USE extends KEYWORD("USE")

  case object USER extends KEYWORD("USER")

  case object USER_RESOURCES extends KEYWORD("USER_RESOURCES")

  case object USE_FRM extends KEYWORD("USE_FRM")

  case object USING extends KEYWORD("USING")

  case object UTC_DATE extends KEYWORD("UTC_DATE")

  case object UTC_TIME extends KEYWORD("UTC_TIME")

  case object UTC_TIMESTAMP extends KEYWORD("UTC_TIMESTAMP")

  case object VALIDATE extends KEYWORD("VALIDATE")

  case object VALIDATION extends KEYWORD("VALIDATION")

  case object VALUE extends KEYWORD("VALUE")

  case object VALUES extends KEYWORD("VALUES")

  case object VARBINARY extends KEYWORD("VARBINARY")

  case object VARCHAR extends KEYWORD("VARCHAR")

  case object VARCHARACTER extends KEYWORD("VARCHARACTER")

  case object VARIABLES extends KEYWORD("VARIABLES")

  case object VARYING extends KEYWORD("VARYING")

  case object VCPU extends KEYWORD("VCPU")

  case object VERBOSE extends KEYWORD("VERBOSE")

  case object VERSION extends KEYWORD("VERSION")

  case object VIEW extends KEYWORD("VIEW")

  case object VIRTUAL extends KEYWORD("VIRTUAL")

  case object VISIBLE extends KEYWORD("VISIBLE")

  case object WAIT extends KEYWORD("WAIT")

  case object WARNINGS extends KEYWORD("WARNINGS")

  case object WEEK extends KEYWORD("WEEK")

  case object WEIGHT_STRING extends KEYWORD("WEIGHT_STRING")

  case object WHEN extends KEYWORD("WHEN")

  case object WHERE extends KEYWORD("WHERE")

  case object WHILE extends KEYWORD("WHILE")

  case object WINDOW extends KEYWORD("WINDOW")

  case object WITH extends KEYWORD("WITH")

  case object WITHOUT extends KEYWORD("WITHOUT")

  case object WORK extends KEYWORD("WORK")

  case object WRAPPER extends KEYWORD("WRAPPER")

  case object WRITE extends KEYWORD("WRITE")

  case object X509 extends KEYWORD("X509")

  case object XA extends KEYWORD("XA")

  case object XID extends KEYWORD("XID")

  case object XML extends KEYWORD("XML")

  case object XOR extends KEYWORD("XOR")

  case object YEAR extends KEYWORD("YEAR")

  case object YEAR_MONTH extends KEYWORD("YEAR_MONTH")

  case object ZEROFILL extends KEYWORD("ZEROFILL")

  case object ZONE extends KEYWORD("ZONE")

  case object ZONEADD extends KEYWORD("ZONEADD")
}
