# Feature Landscape: Distributed MPP Query Engines

**Domain:** Distributed SQL Query Engines (Trino, Presto, Drill, Spark)
**Researched:** 2026-04-22
**Confidence:** MEDIUM-HIGH (based on official docs + ecosystem analysis)

## Executive Summary

Distributed MPP query engines are purpose-built for OLAP workloads on large datasets. They share a common coordinator-worker architecture but differ in execution models: Trino/Presto use **pipeline streaming** (data flows without materialization between stages), while Spark uses **batch** (materializes stages). The feature set can be categorized as:

- **Table stakes**: Features users expect as baseline — missing these means the product is incomplete
- **Differentiators**: Features that provide competitive advantage — build these to stand out
- **Anti-features**: Things to deliberately NOT build — these are either out-of-scope or handled by specialized tools

## Table Stakes Features

Features users expect. Missing these = product feels incomplete and unusable for OLAP workloads.

### SQL Language Core

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| SELECT with projection, filtering | Basic SQL querying | Low | Must support standard SQL syntax |
| WHERE clause with AND/OR/NOT | Data filtering | Low | Must support boolean expressions |
| GROUP BY with aggregation | Data aggregation | Low | COUNT, SUM, AVG, MIN, MAX |
| HAVING clause | Post-aggregation filtering | Low | Depends on GROUP BY |
| ORDER BY with ASC/DESC | Result ordering | Low | NULL handling semantics matter |
| LIMIT/OFFSET | Pagination | Low | Required for interactive queries |
| JOIN operations (INNER, LEFT, RIGHT, FULL) | Data combining | Medium | Broadcast join for small tables |
| Set operations (UNION, INTERSECT, EXCEPT) | Result combining | Low | Deduplication behavior varies |
| Subqueries (scalar, table, correlated) | Complex logic | Medium | Not always fully optimized |
| Common Table Expressions (CTE/WITH) | Query organization | Low | Improves readability significantly |
| CASE expressions (COALESCE, NVL) | Conditional logic | Low | Essential for data transformation |

### Advanced SQL Capabilities

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Window functions (ROW_NUMBER, RANK, DENSE_RANK, LEAD, LAG) | Analytical queries | Medium | Core OLAP capability |
| Aggregate functions with FILTER (COUNT(*) FILTER WHERE...) | Advanced aggregation | Medium | PostgreSQL-compatible syntax |
| APPROX_PERCENTILE, APPROX_DISTINCT | Large-scale aggregation | Medium | Sketch-based algorithms |
| Date/time functions (date_trunc, EXTRACT, date_diff) | Temporal analysis | Low | Essential for time-series |
| String functions (SUBSTR, CONCAT, REGEXP) | Data cleaning | Low | Variety matters |
| Type conversion functions (CAST, TRY_CAST) | Data handling | Low | Error handling varies |

### Data Sources & Formats

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Parquet file support | Standard analytical format | Medium | Columnar, compressed |
| CSV/TSV file support | Common data exchange | Low | Header handling, delimiters |
| JSON file support | Semi-structured data | Medium | Nested JSON parsing |
| S3-compatible storage | Cloud data lakes | Medium | AWS S3, MinIO, etc. |
| HDFS support | Hadoop ecosystem | Medium | Kerberos authentication |
| Local filesystem | Development/testing | Low | Development convenience |
| PostgreSQL connector | RDBMS federation | Medium | Query pushdown important |
| MySQL connector | RDBMS federation | Medium | Feature parity varies |

### Distributed Execution

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Coordinator-worker architecture | Horizontal scaling | High | Core architectural pattern |
| Distributed query planning | Multi-node execution | High | Fragment-based plan representation |
| Data partitioning awareness | Locality-aware scheduling | High | Reduces network traffic |
| Exchange operators (shuffle, broadcast) | Data movement | High | Network-bound operations |
| Parallel table scan | Parallel I/O | Medium | Split creation per file/partition |
| Hash aggregation/distribution | Parallel grouping/joining | Medium | Memory pressure management |
| Memory management per node | Resource safety | High | Spill-to-disk for large ops |

### Security

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Authentication (password, Kerberos) | Access control | Medium | LDAP/Active Directory integration |
| Authorization ( GRANT/REVOKE) | Row/column security | Medium | Ranger/Starburst support |
| SSL/TLS for connections | Transport security | Low | Encrypt data in transit |
| Query result caching | Performance | Medium | Partial results, invalidation |

### Observability

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| EXPLAIN (query plan) | Query debugging | Low | Distributed EXPLAIN important |
| Query metrics (CPU, memory, rows) | Performance tuning | Medium | Per-stage metrics |
| Distributed query profile | Deep troubleshooting | High | Visual plan analysis |
| Logging (query, access, error) | Audit and debug | Low | Log levels matter |

### Client Interfaces

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| JDBC driver | BI tool compatibility | Medium | Type 4 driver standard |
| CLI (command-line interface) | Developer experience | Low | Interactive and batch modes |
| HTTP/REST API | Thin client, integration | Low | Web-based tooling |
| SQLAlchemy integration | Python ecosystem | Low | Pandas integration via SQL |

---

## Differentiators

Features that set products apart. Not expected, but valued. Build these for competitive advantage.

### Query Optimization Sophistication

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Cost-based optimizer (CBO) | Better plan selection | Very High | Statistics-based |
| Adaptive query planning | Handle runtime surprises | High | Re-plan based on actual data |
| Predicate pushdown | Filter early, reduce data | High | File-level, row-level, column pruning |
| Projection pushdown | Read only needed columns | Medium | Parquet column pruning |
| Aggregation pushdown | Push to data source | High | Leverage source capabilities |
| Join reorder with dynamic filtering | Optimal join ordering | High | Bushy plans for complex joins |
| Distributed memory-aware planning | Avoid OOM on large queries | Very High | Inter-node memory pressure |

### Lakehouse & Schema Evolution

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Apache Iceberg support | Open table format, time travel | High | ACID transactions, partition evolution |
| Delta Lake support | ACID on data lakes | High | Databricks ecosystem |
| Hudi support | Incremental processing | High | CDC patterns |
| Schema evolution/drift handling | Evolving data | Medium | ALTER TABLE additions |
| Time travel queries | Historical data access | Medium | Snapshot isolation |

### Advanced SQL & Semi-Structured Data

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Nested data access (arrays, maps) | Schema flexibility | High | FLATTEN, KVGEN functions |
| Complex type constructors | Array/map construction | Medium | ARRAY[], MAP[] |
| Lateral joins | Correlated subqueries | High | UNNEST with correlations |
| PIVOT/UNPIVOT | Cross-tab analysis | Medium | Rotation transformations |
| MATCH_RECOGNIZE | Pattern matching | High | Complex event processing |
| Geospatial functions (ST_*) | GIS analytics | Medium | PostGIS-compatible |
| Row-level security | Multi-tenant isolation | Medium | Policy-based access |

### Connector Ecosystem

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| 40+ connector ecosystem (Trino) | Query everything | High | breadth of data sources |
| Elasticsearch/OpenSearch | Full-text search queries | Medium | Query pushdown |
| Kafka (read-only for now) | Log analysis | Medium | Not real-time streaming |
| Redis/Memcached | Cache integration | Low | Key-value lookups |
| Snowflake/BigQuery/Redshift | Cloud data warehouses | Medium | Query federation |
| Apache Druid | Timeseries data | Medium | Pre-aggregated data |

### Performance & Resource Management

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Pipeline execution (Trino-style) | Low latency, no stage materialization | Very High | Core architectural differentiator |
| Vectorized execution | SIMD, cache-friendly | Very High | Column-at-a-time processing |
| Memory-aware operators | Graceful degradation | High | Spill to disk for large sorts |
| Concurrency limits | Multi-tenant fairness | Medium | Query queuing |
| Resource groups | Workload management | Medium | Priority-based scheduling |
| Result caching (distributed) | Repeated query speedup | High | Invalidation strategies |
| Prepared statements | Plan reuse, security | Low | Reduces parsing overhead |

### UDF/UDTF Extensibility

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Scalar UDFs | Custom transformations | Low | Per-row operations |
| Aggregate UDAFs | Custom aggregations | Medium | Multi-phase aggregation |
| Table UDTFs | Lateral view generation | Medium | Returns dynamic result sets |
| JavaScript/Python UDFs | Language flexibility | High | Security sandboxing |
| Inline function definitions | Rapid prototyping | Low | CREATE FUNCTION in query |

### High Availability

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Multi-coordinator | Coordinator HA | High | Active-passive or Raft |
| Graceful coordinator failover | Query continuity | High | Re-attempt running queries |
| Worker failure handling | Partial query retry | Medium | Fast-fail vs checkpoint |
| ZooKeeper-less coordination | Simplified ops | High | Self-hosted Raft |

---

## Anti-Features

Features to explicitly NOT build. These are out-of-scope or handled by specialized tools.

| Anti-Feature | Why Avoid | What To Do Instead |
|--------------|-----------|-------------------|
| OLTP/Transaction processing | Not designed for high-frequency single-row updates | Use PostgreSQL, MySQL for OLTP |
| Full ACID transactions across sources | Distributed 2PC is slow, not needed for analytics | Single-source transactions via connectors |
| Real-time Kafka streaming | Requires incremental ingestion, not batch queries | Use Flink, Kafka Streams for streaming |
| Native graph processing | Not optimized for graph traversal | Use Neo4j, GraphX for graph |
| ML/AI model training | Separate workload, different optimization | Use MLlib, TensorFlow on Spark |
| Fine-grained checkpoint recovery | Complexity outweighs benefit for OLAP | Fast-fail with task retry |
| Graphical query builder | Not primary interface | Leave for BI tool integration |
| Multi-language SDK | HTTP API sufficient for v1 | Avoid complexity of Python/Go/JS SDKs |
| Real-time incremental processing | CDC patterns, watermark management | Batch-first, defer to streaming systems |
| General-purpose database replacement | Different use case, different tradeoffs | Integrate via connectors |

---

## Feature Dependencies

```
SQL Core (SELECT, GROUP BY, JOIN) → Window Functions
                                → Aggregate Functions
                                → Set Operations

Distributed Execution → Exchange Operators
                     → Memory Management
                     → Query Planning

Connectors → Predicate Pushdown
           → Type Conversion
           → Federated Queries

Lakehouse Support → Iceberg/Delta/Hudi Metadata
                 → Time Travel
                 → Schema Evolution

Pipeline Execution → Exchange Operators (no materialization)
                  → Streaming Aggregation
                  → Continuous Optimization
```

---

## MVP Recommendation

Prioritize in this order for a working OLAP engine:

### Phase 1: Core (Must Have)
1. SQL SELECT, WHERE, GROUP BY, ORDER BY, LIMIT
2. Basic aggregates (COUNT, SUM, AVG, MIN, MAX)
3. JOIN operations (broadcast hash join)
4. Parquet + CSV file support
5. S3 storage connector
6. Coordinator-worker distributed execution
7. Pipeline exchange operators
8. Basic memory management
9. JDBC driver
10. CLI interface

### Phase 2: Essential (Table Stakes)
1. Window functions (ROW_NUMBER, RANK, LEAD, LAG)
2. Additional connectors (PostgreSQL, MySQL)
3. Window functions with frames
4. EXPLAIN (distributed query plan)
5. Date/time functions
6. String manipulation functions
7. Type casting
8. CASE/NULL handling
9. CTE support
10. Basic query metrics

### Phase 3: Differentiators (Competitive Edge)
1. Cost-based optimizer rules
2. Iceberg connector + time travel
3. Advanced pushdown (predicate, aggregation)
4. Dynamic filtering
5. UDF/UDTF support
6. Resource groups / query queuing
7. Memory-aware operators with spill
8. Prepared statements
9. Multi-coordinator HA

---

## Sources

- [Trino Documentation (Official)](https://trino.io/docs/current/) — HIGH confidence
- [Presto Documentation (Official)](https://prestodb.io/docs/current/) — HIGH confidence
- [Apache Drill Documentation (Official)](https://drill.apache.org/docs/) — HIGH confidence
- [Apache Spark Documentation (Official)](https://spark.apache.org/docs/4.1.1/) — HIGH confidence
- [Trino SQL Functions Reference](https://github.com/trinodb/trino/blob/master/docs/src/main/sphinx/functions/window.md) — HIGH confidence
