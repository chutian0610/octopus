# Roadmap: Octopus

## Overview

Octopus is a distributed MPP query engine built on Apache DataFusion, targeting Trino-style streaming/pipeline execution for sub-second OLAP queries on large datasets. The journey builds from single-node SQL execution through distributed coordinator-worker architecture to full observability and client interfaces.

**Phase order rationale:** Single-node foundation first (correct streaming patterns), then coordinator (communication backbone), then workers/Arrow Flight (data plane), then advanced SQL and federated sources, finally observability and client tooling.

## Phases

- [x] **Phase 1: Single-Node Foundation** - Local SQL execution on Parquet/CSV/JSON with DataFusion
- [ ] **Phase 2: Coordinator Core** - Distributed query planning, gRPC control plane, CLI interface
- [ ] **Phase 3: Workers + Arrow Flight** - Parallel task execution, Exchange operators, data transfer
- [ ] **Phase 4: Advanced SQL & Federated Sources** - Window functions, PostgreSQL/MySQL connectors
- [ ] **Phase 5: Observability & Clients** - Metrics, JDBC driver, UDF support

## Phase Details

### Phase 1: Single-Node Foundation
**Goal**: Users can execute SQL queries locally on Parquet/CSV/JSON files with Rust-level performance
**Depends on**: Nothing (first phase)
**Requirements**: SQL-01, SQL-02, SQL-03, SQL-04, SQL-05, DATA-01, DATA-02, DATA-03, OBS-03
**Success Criteria** (what must be TRUE):
  1. User can execute SELECT with projection, filtering (WHERE), GROUP BY, ORDER BY, LIMIT/OFFSET
  2. User can use aggregation functions (COUNT, SUM, AVG, MIN, MAX) with GROUP BY and HAVING
  3. User can perform JOIN operations (INNER, LEFT, RIGHT, FULL) with broadcast hash join
  4. User can use set operations (UNION, INTERSECT, EXCEPT) and subqueries (scalar, table, correlated)
  5. User can use CTE (WITH clause) for query organization
  6. User can query Parquet files on S3/HDFS/local filesystem
  7. User can query CSV/TSV files on S3/HDFS/local filesystem
  8. User can query JSON files on S3/HDFS/local filesystem
  9. System provides structured logging with query tracing
**Plans**: 3 plans
Plans:
- [x] 01-01-PLAN.md — Workspace foundation + DataFusion session
- [x] 01-02-PLAN.md — SQL execution (SELECT, aggregation, JOIN, CTE)
- [x] 01-03-PLAN.md — File formats (Parquet/CSV/JSON) + structured logging

### Phase 2: Coordinator Core
**Goal**: Coordinator parses SQL and creates distributed query plan; users can submit queries via CLI
**Depends on**: Phase 1
**Requirements**: DIST-01, DIST-02, CLI-01, OBS-01
**Success Criteria** (what must be TRUE):
  1. Coordinator can parse SQL and create distributed query plan
  2. Coordinator can assign tasks to workers based on data locality
  3. User can use CLI to execute SQL queries interactively or in batch mode
  4. User can execute EXPLAIN to see distributed query plan
**Plans**: 5 plans
Plans:
- [x] 02-01-PLAN.md — Coordinator core with worker registry, query scheduler, CLI modes
- [x] 02-02-PLAN.md — DataFusion SQL parsing and data locality scheduling (gap closure)
- [x] 02-03-PLAN.md — REPL execution, batch mode, and EXPLAIN command (gap closure)

### Phase 3: Workers + Arrow Flight
**Goal**: Workers execute tasks in parallel and exchange data via Arrow Flight with pipeline execution
**Depends on**: Phase 2
**Requirements**: DIST-03, DIST-04, DIST-05, OBS-02
**Success Criteria** (what must be TRUE):
  1. Workers can execute tasks in parallel and exchange data via Arrow Flight
  2. Exchange operators can pipeline data without stage materialization
  3. System supports task-level retry on failure (configurable)
  4. System emits metrics (CPU, memory, rows processed per stage)
**Plans**: 4 plans
Plans:
- [ ] 03-01-PLAN.md — Worker service foundation with CPU/IO runtime separation
- [ ] 03-02-PLAN.md — Arrow Flight data plane with worker-pull model
- [ ] 03-03-PLAN.md — Exchange operators with pipeline streaming and backpressure
- [ ] 03-04-PLAN.md — Task retry mechanism and metrics collection

### Phase 4: Advanced SQL & Federated Sources
**Goal**: Users can query PostgreSQL/MySQL and use window functions and advanced SQL features
**Depends on**: Phase 3
**Requirements**: DATA-04, DATA-05, ADV-01, ADV-02, ADV-03
**Success Criteria** (what must be TRUE):
  1. User can query PostgreSQL via federated connector
  2. User can query MySQL via federated connector
  3. User can use window functions (ROW_NUMBER, RANK, DENSE_RANK, LEAD, LAG) with frames
  4. User can use date/time functions (date_trunc, EXTRACT, date_diff) and string functions (SUBSTR, CONCAT, REGEXP)
  5. User can use type conversion functions (CAST, TRY_CAST) and CASE/COALESCE/NVL expressions
**Plans**: TBD

### Phase 5: Observability & Clients
**Goal**: Full observability with metrics, logging, and client interfaces (JDBC, UDF)
**Depends on**: Phase 4
**Requirements**: ADV-04, OBS-02, CLI-02
**Success Criteria** (what must be TRUE):
  1. User can use UDF/UDTF for custom transformations
  2. System emits metrics (CPU, memory, rows processed per stage)
  3. User can use JDBC driver to connect from BI tools
**Plans**: TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Single-Node Foundation | 3/3 | Complete | 2026-05-07 |
| 2. Coordinator Core | 3/3 | Complete | - |
| 3. Workers + Arrow Flight | 0/4 | In progress | - |
| 4. Advanced SQL & Federated Sources | 0/5 | Not started | - |
| 5. Observability & Clients | 0/3 | Not started | - |

---

*Roadmap created: 2026-04-22*