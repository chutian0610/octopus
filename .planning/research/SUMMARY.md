# Project Research Summary

**Project:** Octopus — Distributed MPP Query Engine
**Domain:** Distributed SQL Query Engines (Trino-style streaming OLAP)
**Researched:** 2026-04-22
**Confidence:** HIGH

## Executive Summary

Octopus is a distributed MPP query engine built on Apache DataFusion, targeting Trino-style streaming/pipeline execution rather than Spark's batch model. The architecture follows a coordinator-worker pattern where the coordinator parses SQL, creates a stage DAG with Exchange operators, and assigns tasks to workers; workers execute in parallel and exchange intermediate data via Arrow Flight. The stack is Rust-native: DataFusion 53.x for query execution, tonic 0.14 for gRPC control plane, arrow-flight 58 for high-speed data transfer, and Tokio 1.52 as the async runtime.

**Recommended approach:** Start with single-node DataFusion for SQL parsing and execution, then layer distributed planning (Exchange operators, stage DAG) on top. The critical insight from research is that pipeline breakers (full sort, hash aggregation with multiple groups) must be handled explicitly — they block streaming and cause memory issues. Separate Tokio runtimes for CPU-bound compute and I/O-bound Flight operations are essential from day one.

**Key risks:** Pipeline breaker ignorance causes hangs; Tokio runtime contention causes p99 latency spikes; Arrow Flight batch size mismanagement causes OOM; Exchange operator deadlock causes hangs with no error messages. These are all addressable with proper design, but must be architected in from the start.

## Key Findings

### Recommended Stack

Octopus builds on proven Apache DataFusion ecosystem with a clean separation between control plane (gRPC via Tonic) and data plane (Arrow Flight). The stack mirrors Ballista's architecture but targets streaming/pipeline execution.

**Core technologies:**
- **DataFusion 53.x**: Production-grade extensible query engine — SQL parsing, optimization, execution. Non-negotiable.
- **Tokio 1.52**: Async runtime with `rt_multi_thread` for workers, `spawn_blocking` for CPU-intensive ops. Must configure separate runtimes for CPU vs I/O.
- **tonic 0.14 + prost 0.14**: gRPC framework and protobuf code generation. Standard for Rust.
- **arrow-flight 58.1.0**: High-speed columnar data transfer via gRPC streams. Zero-copy where possible.
- **sqlx 0.8**: Async database driver with compile-time checked queries for metadata storage. Plugable trait architecture.
- **object_store 0.13**: Unified S3/HDFS/Azure/GCS access. Used by DataFusion.

### Expected Features

**Must have (table stakes):**
- SQL core: SELECT, WHERE, GROUP BY, ORDER BY, LIMIT, JOIN (broadcast hash), CTEs
- File formats: Parquet, CSV, JSON over S3/HDFS/local
- Coordinator-worker distributed execution with Exchange operators
- Pipeline streaming (Trino-style, not Spark batch)
- JDBC driver + CLI interface
- Basic query metrics and EXPLAIN

**Should have (competitive):**
- Window functions (ROW_NUMBER, RANK, LEAD, LAG)
- Additional connectors (PostgreSQL, MySQL)
- Predicate/aggregation pushdown for data locality
- Iceberg connector + time travel
- Resource groups / query queuing

**Defer (v2+):**
- Cost-based optimizer (CBO) — very high complexity
- Multi-coordinator HA — high complexity
- Full fault tolerance with partial results
- JavaScript/Python UDFs

### Architecture Approach

Trino-style streaming architecture where data flows through operators without materialization between stages. Exchange operators are the **only points where data crosses worker boundaries** — everywhere else, data stays local and streaming. The coordinator produces a stage DAG (not a linear sequence); each stage is a set of tasks that run in parallel on multiple workers.

**Major components:**
1. **Coordinator** — SQL parsing, logical/physical planning, distributed stage DAG planning, task scheduling, result aggregation, catalog/metadata
2. **Workers** — Execute assigned tasks in parallel, exchange data via Exchange operators, report completion/failure to coordinator
3. **Query Planner (Octopus extension)** — Extends DataFusion with partition planning, Exchange insertion, pushdown rules, data locality
4. **Exchange Operator** — Data movement between pipeline stages; GATHER (final agg), REPARTITION (hash shuffle), REPLICATE (broadcast)
5. **Scheduler** — Assigns tasks to workers based on data locality and load, handles work stealing

### Critical Pitfalls

1. **Pipeline Breaker Ignorance** — Full sort, hash aggregation with many groups, collect-all joins must consume all input before producing output. Breaks streaming semantics. Prevention: Mark `unbounded_output()` correctly, design Exchange to handle backpressure, create pipeline breaker detector rule.

2. **Tokio Runtime Contention** — S3/HDFS reads block compute threads when CPU and I/O share thread pool. Causes p99 >> p50 latency. Prevention: Separate Tokio runtimes for CPU-bound execution and I/O-bound Flight/object store.

3. **Arrow Flight Batch Size Mismanagement** — Unbounded batches cause OOM (e.g., 1M rows with large strings = >10GB). Prevention: Set `max_batch_size` (~100K rows), bounded channel receivers on coordinator.

4. **Partition Pruning Ignorance** — Queries scan all partitions even when filters should limit scope. Causes 10-100x excess network traffic. Prevention: Store partition metadata (min/max), implement partition pruning in task scheduling.

5. **Exchange Operator Deadlock** — Cyclic dependencies through Exchange operators cause distributed deadlock. Prevention: DAG validation before execution, non-blocking Exchange with bounded channels, deadlock detection timeout.

## Implications for Roadmap

Based on research, suggested phase structure aligns with Architecture.md's build order and avoids critical pitfalls at each stage.

### Phase 1: Single-Node DataFusion Foundation
**Rationale:** All distributed execution builds on single-node query execution. Must establish correct patterns (streaming operators, runtime separation) before distribution.
**Delivers:** Local SQL execution against Parquet/CSV files, proper `unbounded_output()` on operators, separate Tokio runtimes for CPU/IO.
**Addresses:** FEATURES.md — SQL core, basic file formats
**Avoids:** Pitfall 1 (pipeline breakers), Pitfall 2 (runtime contention)

### Phase 2: Coordinator Core
**Rationale:** Coordinator owns logical-to-distributed planning boundary. Must be built before workers can receive tasks.
**Delivers:** gRPC control plane service, worker registration, task creation/distribution, stage DAG planning with Exchange insertion.
**Uses:** Stack — tonic 0.14, prost 0.14, async-trait
**Implements:** Architecture — Coordinator component, control plane communication

### Phase 3: Worker Execution + Arrow Flight Data Plane
**Rationale:** Workers must receive tasks and exchange data. Arrow Flight is the data transfer mechanism for Exchange operators.
**Delivers:** Worker service, task execution, Flight data plane with batch size limits, simple 2-stage query (Scan → Aggregate with one Exchange).
**Avoids:** Pitfall 3 (batch size OOM), Pitfall 5 (exchange deadlock via bounded channels)

### Phase 4: Distributed Optimization
**Rationale:** Basic execution working — now add intelligence. Partition metadata enables locality-aware scheduling.
**Delivers:** Partition metadata in catalog, data locality scheduler, predicate pushdown, exchange optimization, partition pruning.
**Avoids:** Pitfall 4 (partition pruning/locality failures), Pitfall 7 (load imbalance via work stealing)

### Phase 5: Fault Tolerance & HA
**Rationale:** Execution foundation solid — now handle failures gracefully.
**Delivers:** Task retry with fast-fail, heartbeat monitoring, graceful degradation, partial result handling.
**Avoids:** Pitfall 9 (session state explosion via limits), Pitfall 10 (graceful degradation)

### Phase Ordering Rationale

- **Single-node first**: Distributed adds significant complexity. Establish correct single-node patterns (streaming, runtime separation) before distribution. Architecture.md explicitly lists this as Phase 1.
- **Coordinator before workers**: Workers are stateless executors — they need something to connect to. gRPC control plane is the communication backbone.
- **Data plane after task execution**: Arrow Flight is the transport for Exchange operators. Must have tasks producing data before Flight transfer matters.
- **Optimization after basic execution**: Partition pruning and locality-aware scheduling require working execution to test. Don't optimize what doesn't work yet.
- **Fault tolerance last**: Requires working system to test failure scenarios.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3 (Arrow Flight data plane):** Complex gRPC stream management, connection pooling, batch sizing — needs API research and spike
- **Phase 5 (Fault Tolerance):** Multi-coordinator consensus patterns, split-brain prevention — sparse documentation, needs research-phase

Phases with standard patterns (skip research-phase):
- **Phase 1 (DataFusion foundation):** Well-documented, established patterns from DataFusion examples
- **Phase 2 (Coordinator):** gRPC service patterns well-established via tonic examples

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Verified against DataFusion 53 workspace dependencies, Ballista Cargo.toml, Arrow/tonic docs |
| Features | MEDIUM-HIGH | Based on official docs for Trino/Presto/Drill/Spark; some inference for MVP prioritization |
| Architecture | HIGH | Based on DataFusion official docs, Trino architecture, SIGMOD 2024 paper, Flink patterns |
| Pitfalls | MEDIUM-HIGH | Based on DataFusion docs + distributed systems patterns; some inferred prevention strategies |

**Overall confidence:** HIGH

### Gaps to Address

- **Metadata store trait design**: Concrete implementation choice (PostgreSQL vs SQLite vs etcd) not made — defer to implementation phase based on deployment requirements
- **Coordinator-worker protocol details**: Task serialization format, heartbeat frequency, retry policies — need specification during Phase 2 planning
- **Batch size tuning**: 100K rows default needs empirical validation — spike during Phase 3
- **Load balancing algorithm**: Work stealing vs weighted assignment — needs implementation testing

## Sources

### Primary (HIGH confidence)
- [DataFusion 53 Cargo.toml](https://raw.githubusercontent.com/apache/datafusion/refs/heads/main/Cargo.toml) — Stack verification
- [DataFusion Architecture Docs](https://docs.rs/datafusion/latest/datafusion/) — Pipeline breakers, streaming execution
- [DataFusion SIGMOD 2024 Paper](https://dl.acm.org/doi/10.1145/3626246.3653368) — Architecture decisions
- [Arrow Flight Protocol](https://github.com/apache/arrow/blob/main/docs/source/format/Flight.md) — Data plane design
- [Tokio Thread Pool Best Practices](https://tokio.rs/blog/2020-04-preemption) — Runtime isolation
- [Trino Architecture (Official)](https://trino.io/docs/current/overview/concepts) — Streaming architecture reference
- [Arrow Flight RPC Guide](https://arrow.apache.org/docs/cpp/flight.html) — Batch sizing, connection management

### Secondary (MEDIUM-HIGH confidence)
- [Ballista Architecture](https://github.com/apache/arrow-datafusion) — Coordinator-worker patterns, known issues
- [Trino Documentation (Official)](https://trino.io/docs/current/) — Feature expectations
- [Presto Documentation (Official)](https://prestodb.io/docs/current/) — Feature expectations

### Tertiary (MEDIUM confidence)
- [Flink Runtime Architecture](https://github.com/apache/flink/blob/master/docs/content/docs/concepts/flink-architecture.md) — Streaming-first MPP patterns

---
*Research completed: 2026-04-22*
*Ready for roadmap: yes*
