# Domain Pitfalls: Distributed MPP Query Engine (Octopus)

**Project:** Octopus — Distributed MPP Query Engine
**Researched:** 2026-04-22
**Confidence:** MEDIUM-HIGH (based on DataFusion official docs + distributed systems patterns)

---

## Critical Pitfalls

Mistakes that cause rewrites, data corruption, or catastrophic performance degradation.

### Pitfall 1: Pipeline Breaker Ignorance

**What goes wrong:** Queries hang indefinitely or use excessive memory because operators that must materialize all input block the streaming pipeline.

**Why it happens:** DataFusion's streaming execution model requires operators to produce output incrementally. Certain operators — full sort, hash aggregation with multiple groups, collect-all joins — are "pipeline breakers" that must consume all input before producing any output. In a Trino-style streaming architecture, these create backpressure that defeats the purpose of pipeline execution.

**Consequences:**
- Queries that should complete in milliseconds consume gigabytes of memory
- Streaming semantics break — downstream operators sit idle waiting for complete input
- Latency becomes unpredictable (p99 spikes)

**Prevention:**
- Mark all ExecutionPlan operators with `unbounded_output()` returning `true` for streaming operators, `false` for breakers
- Design Exchange operators to handle backpressure from pipeline breakers
- Create a "pipeline breaker detector" query planning rule that warns when a plan has chained breakers
- Consider partial aggregation (streaming) vs. full hash aggregation as separate physical operators

**Detection:**
```
# Warning sign in query plans
explain(analyze) show operators with "N/A" for output rows until input complete
Memory metrics spike when pipeline breaker executes
```

**Phase mapping:** Physical planning phase — must address when implementing Exchange operators and custom physical operators.

---

### Pitfall 2: Tokio Runtime Contention (CPU + IO on Same Thread Pool)

**What goes wrong:** Query latency spikes unpredictably; S3/HDFS reads block compute threads; p99 latency 10x higher than p50.

**Why it happens:** DataFusion uses Tokio as its thread pool for both CPU-intensive compute AND network I/O (Arrow Flight, object store reads). When a long S3 read blocks a tokio thread, all other tasks on that thread — including other query operators — are stalled. This is especially damaging under concurrent query load.

**Consequences:**
- Long tail latencies (p99 >> p50) for queries reading from remote storage
- Network flow control throttling from blocked Flight connections
- Poor utilization of CPU during I/O wait

**Prevention:**
- Use separate Tokio runtimes: one for CPU-bound execution, one for I/O (Flight, object store)
- Configure I/O runtime with more threads than CPU runtime
- Use `datafusion-examples/query_planning/thread_pools.rs` as reference implementation
- For object store reads, use async streaming with bounded channels to decouple I/O from compute

**Detection:**
```
# Warning signs
- p99 latency >> p50 latency under load
- Thread pool utilization metrics show some threads 100% busy, others idle
- Flight connection timeout errors during concurrent queries
```

**Phase mapping:** Execution engine phase — when integrating DataFusion execution with Arrow Flight data plane.

---

### Pitfall 3: Arrow Flight Batch Size Mismanagement

**What goes wrong:** Out-of-memory errors on workers when transferring large batches; memory pressure on coordinator during result aggregation.

**Why it happens:** Arrow Flight transfers `RecordBatch` data directly over gRPC streams. Without proper batch size limits, a single batch can exceed memory limits (e.g., 1M rows with large string columns = >10GB). Additionally, the coordinator may receive many concurrent Flight streams that accumulate in memory before being consumed.

**Consequences:**
- OOM kills on worker nodes during data transfer
- Coordinator memory exhaustion with many concurrent queries
- Network connection drops due to memory pressure

**Prevention:**
- Set `max_batch_size` in Flight DoGet/DoPut requests (e.g., 100K rows default)
- Implement bounded channel receivers on coordinator — do not accumulate Flight data faster than it can be processed
- Use `FlightData::data_body` size limits and reject batches exceeding threshold
- Monitor Arrow batch memory allocation: `arrow::util::pretty::pretty_format_batches` for debugging

**Detection:**
```
# Warning signs
- Worker OOM logs coincide with Flight transfer completion
- Coordinator RSS grows monotonically during result aggregation
- gRPC stream window exhaustion messages
```

**Phase mapping:** Data plane implementation phase — when implementing Arrow Flight servers and clients.

---

### Pitfall 4: Partition Pruning Ignorance (Data Locality Failures)

**What goes wrong:** Queries scan all partitions even when filter predicates should limit to specific partitions; network traffic 100x expected.

**Why it happens:** Without partition pruning rules, workers request data from partitions that could have been excluded by filter predicates. In a distributed setting, this means unnecessary network transfer between storage and compute. DataFusion supports partition pushdown for `ListingTable`, but distributed scheduling must propagate filter information to task assignment.

**Consequences:**
- 10-100x more data transferred over network than necessary
- Query latency dominated by network I/O, not compute
- Workers waste CPU scanning irrelevant partitions

**Prevention:**
- Ensure partition metadata (min/max values per partition) is available to coordinator
- Implement partition pruning in task scheduling: skip partitions where `predicate_col NOT BETWEEN partition_min AND partition_max`
- Use DataFusion's `PruningPredicate` for bloom filter pruning on Parquet
- Test with `EXPLAIN VERBOSE` to verify partition pruning is applied

**Detection:**
```
# Warning signs
- Query execution time scales with total data size, not filtered data size
- Network bytes transferred >> result data size
- EXPLAIN shows Scan with all partitions, not filtered subset
```

**Phase mapping:** Distributed scheduling phase — when implementing partition-aware task distribution.

---

### Pitfall 5: Exchange Operator Deadlock

**What goes wrong:** Distributed query hangs with all workers idle; no progress, no error messages.

**Why it happens:** In a streaming pipeline with multiple Exchange operators (e.g., for repartitioning mid-query), each Exchange waits for the other to complete. Classic distributed deadlock: Stage 1 waits for Stage 2 to send data, Stage 2 waits for Stage 1's input. This happens when:
- Two stages have a cyclic dependency through Exchange operators
- Backpressure creates circular wait condition
- Task scheduler assigns tasks in a way that creates dependency cycles

**Consequences:**
- Query hangs indefinitely (requires coordinator restart)
- No error output because the query is "running" (just deadlocked)
- All associated workers appear idle but allocated

**Prevention:**
- Implement DAG validation in query planning: detect cycles before execution
- Use non-blocking Exchange with bounded channels — allow senders to fail fast if receiver is blocked
- Set `exchange_concurrency_limit` to prevent too many in-flight batches between stages
- Add deadlock detection timeout: if no progress for X seconds, abort and retry

**Detection:**
```
# Warning signs
- Query stuck at "EXCHANGE" stage in EXPLAIN ANALYZE
- All worker CPU utilization drops to 0% simultaneously
- Query age exceeds expected duration by 10x
```

**Phase mapping:** Query planning and execution phases — when implementing multi-stage pipeline execution with Exchange operators.

---

## Moderate Pitfalls

Design issues that cause significant performance problems or complexity but can be addressed mid-project.

### Pitfall 6: Coercion/Type Mismatch Silent Failures

**What goes wrong:** Queries return wrong results (no error) because of silent type coercion in distributed execution.

**Why it happens:** When distributing query fragments across workers, each worker may resolve types independently. If the coordinator uses a different type coercion rule than workers (e.g., due to different DataFusion versions or configuration), the same expression may evaluate differently. This is especially dangerous with decimal precision, timestamps across timezones, and string/numeric comparisons.

**Prevention:**
- Lock DataFusion version across coordinator and workers
- Centralize all type resolution in coordinator; workers receive fully-typed physical plans
- Add validation stage: execute a "type check query" that verifies result types before returning to client
- Use explicit CAST expressions rather than implicit coercion

**Phase mapping:** Coordinator-worker communication phase — when establishing protocol for plan distribution.

### Pitfall 7: Task Scheduler Load Imbalance

**What goes wrong:** 90% of query work executes on 10% of workers; some nodes idle while others are overloaded.

**Why it happens:** Default round-robin or simple hash-based task distribution doesn't account for:
- Variable data sizes per partition
- Different query complexity (some tasks are CPU-intensive, others are I/O-bound)
- Worker resource heterogeneity (different CPU counts, memory)

**Consequences:**
- Overall query latency determined by slowest worker (straggler problem)
- Poor cluster utilization despite high load on some nodes
- p99 latency >> average latency

**Prevention:**
- Implement work stealing: idle workers steal tasks from busy workers
- Estimate task cost based on data size and operator type
- Track worker load in coordinator and weight task assignment accordingly
- Consider adaptive partitioning: split large partitions into smaller units

**Phase mapping:** Distributed scheduling phase — when implementing task assignment logic.

### Pitfall 8: gRPC Connection Pool Exhaustion

**What goes wrong:** New queries fail with "connection refused" or timeouts even when cluster is healthy; existing queries continue normally.

**Why it happens:** Arrow Flight over gRPC creates long-lived bidirectional streams. Without connection pooling and limits, each query may open multiple Flight connections per worker. Under concurrent query load, gRPC connection limits are hit (default max channels per server: 100, max per client: 10).

**Prevention:**
- Implement Flight connection pooling: reuse connections across queries
- Set `grpc.max concurrent streams` appropriately for expected concurrency
- Monitor connection count and scale coordinators or use connection pooling sidecar
- Use multiplexing (single Flight connection for multiple streams) where possible

**Phase mapping:** Control plane implementation phase — when building gRPC services for coordination.

---

## Minor Pitfalls

Implementation issues with localized impact.

### Pitfall 9: Session State Explosion

**What goes wrong:** Coordinator memory grows unbounded over time; queries gradually slow down; eventually coordinator OOMs.

**Why it happens:** DataFusion's `SessionContext` caches metadata (table schemas, statistics, UDFs). In a long-running coordinator, these caches grow without bounds, especially with many unique SQL queries that create unique expression objects. The `SessionConfig` defaults may allow unbounded cache growth.

**Prevention:**
- Set `session_config` limits: max_cached_tables, expression array limits
- Implement cache eviction policies with TTL
- Monitor `SessionContext` memory via metrics
- Periodically restart coordinator to clear accumulated state (for v1)

### Pitfall 10: Missing Graceful Degradation on Worker Failure

**What goes wrong:** Single worker crash kills the entire query; no partial results returned; clients receive cryptic gRPC error.

**Why it happens:** Without task-level retry and partial result handling, a failure in any worker task fails the entire query. For analytical workloads where approximate answers may be acceptable, this is especially problematic.

**Prevention:**
- Implement fast-fail: when a task fails, cancel sibling tasks immediately
- For fault-tolerant queries: track which partitions were completed; allow retry of failed partitions
- Return partial results with error metadata to client (not just "query failed")
- Use timeout per task, not per query

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| DataFusion Integration | Thread contention (Pitfall 2) | Separate IO and CPU runtimes from day 1 |
| Exchange Operators | Deadlock (Pitfall 5) | Design DAG validation early |
| Arrow Flight Data Plane | Batch size OOM (Pitfall 3) | Implement bounded channels and batch limits |
| Task Scheduling | Load imbalance (Pitfall 7) | Plan for work stealing, not just assignment |
| Partition Pruning | Locality failures (Pitfall 4) | Build partition metadata into metadata store |
| Multi-Coordinator | Split-brain on consensus | Define single writer for metadata; read can be HA |

---

## Key Reference Sources

| Source | Relevance | Confidence |
|--------|-----------|------------|
| [DataFusion Architecture Docs](https://docs.rs/datafusion/latest/datafusion/) | Pipeline breakers, streaming execution, thread scheduling | HIGH — Official docs |
| [DataFusion SIGMOD 2024 Paper](https://dl.acm.org/doi/10.1145/3626246.3653368) | Architecture decisions, extension points | HIGH — Peer-reviewed |
| [Arrow Flight RPC Guide](https://arrow.apache.org/docs/cpp/flight.html) | Batch sizing, connection management | HIGH — Official docs |
| [DataFusion Ballista Architecture](https://github.com/apache/arrow-datafusion) | Coordinator-worker patterns, known issues | MEDIUM — Community implementation |
| [Tokio Thread Pool Best Practices](https://tokio.rs/blog/2020-04-preemption) | Runtime isolation, cooperative scheduling | HIGH — Official |

---

## Verification Checklist

- [ ] Each pitfall has warning signs (detection method)
- [ ] Each pitfall has prevention strategy (actionable)
- [ ] Each pitfall maps to at least one phase
- [ ] Pitfalls are specific to distributed MPP query engine domain (not generic Rust or async advice)
- [ ] No pitfall contradicts DataFusion's documented behavior
