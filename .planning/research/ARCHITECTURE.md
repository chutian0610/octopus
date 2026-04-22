# Architecture: Distributed MPP Query Engine (Octopus)

**Project:** Octopus
**Domain:** Distributed SQL Query Engines (Trino-style streaming)
**Researched:** 2026-04-22
**Confidence:** HIGH (based on DataFusion official docs + Trino/Spark/Flink ecosystem analysis)

---

## Executive Summary

Octopus follows a **Trino-style streaming architecture** where data flows through operators in a pipeline without materialization between stages. This differs from Spark/Ballista's batch model where each stage completes fully before the next begins. The core architectural pattern is:

- **Coordinator** parses SQL, creates distributed query plan as stages, assigns tasks to workers
- **Workers** execute tasks in parallel, exchange intermediate data via Exchange operators
- **Pipeline execution** keeps all stages running concurrently, streaming data as it's produced
- **Control plane** (gRPC) handles task scheduling and cluster management
- **Data plane** (Arrow Flight) handles high-speed data transfer between workers

The critical insight: **Exchange operators are the only points where data crosses worker boundaries** — everywhere else, data stays local and streaming.

---

## Major Components

### 1. Coordinator

**Responsibility:** Central brain — receives SQL, plans execution, orchestrates workers.

| Function | Description |
|----------|-------------|
| SQL Parsing | Parse SQL text into AST via DataFusion SQL planner |
| Logical Planning | Build logical plan tree (Relation → Filter → Aggregate → Project) |
| Distributed Planning | Convert logical plan to stage DAG with Exchange boundaries |
| Task Scheduling | Assign splits/tasks to workers based on data locality |
| Result Aggregation | Collect final results from workers, return to client |
| Catalog/Metadata | Manage table schemas, partition info, statistics |

**Key Design:** The coordinator produces a **stage DAG** (not a linear sequence). Each stage is a set of tasks that can run in parallel on multiple workers.

```
┌─────────────────────────────────────────────────────────────────┐
│                         COORDINATOR                              │
├─────────────────────────────────────────────────────────────────┤
│  SQL Input                                                       │
│     │                                                            │
│     ▼                                                            │
│  ┌─────────┐    ┌──────────────┐    ┌───────────────────────┐    │
│  │ Parser  │───▶│ Logical Plan │───▶│ Distributed Planner   │    │
│  └─────────┘    └──────────────┘    └───────────────────────┘    │
│                                              │                    │
│                                              ▼                    │
│                                   ┌───────────────────────┐       │
│                                   │   Stage DAG Plan      │       │
│                                   │  (Fragment 0)        │       │
│                                   │       │               │       │
│                                   │  ┌────┴────┐           │       │
│                                   │  │Stage 1  │◀──RemoteSource    │
│                                   │  └────┬────┘           │       │
│                                   │       │                 │       │
│                                   │  ┌────┴────┐           │       │
│                                   │  │Stage 2  │◀──RemoteSource    │
│                                   └────────────────────────────────    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    Task Scheduler                         │    │
│  │  Assigns tasks to workers based on partition locality     │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

**Component Boundary:** The coordinator owns the **logical-to-distributed planning boundary**. It produces physical plans that workers execute. Workers never do their own planning.

### 2. Workers

**Responsibility:** Execute tasks in parallel, exchange data with other workers.

| Function | Description |
|----------|-------------|
| Task Execution | Run assigned tasks (scan, filter, aggregate, join) |
| Data Exchange | Send/receive intermediate data via Exchange operators |
| Local Processing | Use DataFusion for single-partition execution |
| Memory Management | Spill to disk when intermediate data exceeds memory |
| Health Reporting | Report task completion/failure to coordinator |

**Key Design:** Workers are **stateless across queries** — each query spawns new tasks. Workers don't own partition data; they process splits assigned by coordinator.

```
┌─────────────────────────────────────────────────────────────────┐
│                          WORKER NODE                             │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                          │
│  │ Task 1  │  │ Task 2  │  │ Task 3  │  ◀── Parallel execution │
│  │ (Scan)  │  │(Filter) │  │(Agg)    │                          │
│  └────┬────┘  └────┬────┘  └────┬────┘                          │
│       │            │            │                               │
│       └────────┬────┴────────────┘                               │
│                ▼                                                  │
│         ┌─────────────┐                                          │
│         │ Local Cache │  ◀── Recently accessed partitions        │
│         └─────────────┘                                          │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │              Arrow Flight Server                         │     │
│  │  - do_get(): Serve partition data to other workers      │     │
│  │  - do_put(): Receive intermediate results                │     │
│  └─────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

**Component Boundary:** Workers own the **partition-to-task execution boundary**. Given a split and a physical plan, the worker executes and produces RecordBatch streams.

### 3. Query Planner (Optimizer Extension)

**Responsibility:** Extend DataFusion optimizer with distributed-aware rules.

| Function | Description |
|----------|-------------|
| Partition Planning | Determine optimal partitioning scheme for each operation |
| Exchange Insertion | Insert Exchange operators at stage boundaries |
| Pushdown Rules | Push filters/projections closer to data sources |
| Data Locality | Ensure tasks run where data resides |

**Key Design:** DataFusion provides the **single-node physical planner**. Octopus adds a **distributed planning layer** that:
1. Takes DataFusion's physical plan
2. Identifies shuffle boundaries (where data must move between workers)
3. Inserts Exchange operators
4. Assigns partitioning schemes

```
Logical Plan
     │
     ▼
DataFusion Physical Planner
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│              Distributed Planner (Octopus Extension)          │
├─────────────────────────────────────────────────────────────┤
│  1. Identify Exchange boundaries (Repartition, Broadcast)   │
│  2. Assign partitioning schemes to each stage              │
│  3. Insert RemoteSource/RemoteSink operators                │
│  4. Generate task-level physical plans per partition       │
└─────────────────────────────────────────────────────────────┘
     │
     ▼
Stage DAG with Exchange Operators
```

**Component Boundary:** The planner extension sits **between DataFusion's physical plan and worker task execution**. It transforms single-node plans into distributed plans.

### 4. Exchange Operator

**Responsibility:** Data movement between pipeline stages across workers.

Exchange is the **only operator that crosses worker boundaries** in a streaming pipeline.

| Exchange Type | When Used | Data Flow |
|--------------|-----------|-----------|
| **GATHER** | Final stage aggregation | All workers → Coordinator |
| **REPARTITION** | Hash-based reshuffle | Workers exchange based on partition key |
| **REPLICATE** | Broadcast join | One worker → All workers |

```rust
// DataFusion ExecutionPlan interface for Exchange
pub trait ExecutionPlan {
    fn execute(
        &self,
        partition: usize,
        context: Arc<TaskContext>,
    ) -> Result<SendableRecordBatchStream>;

    // For Exchange: partitioning scheme determines data distribution
    fn partitioning(&self) -> Partitioning;
}

// Example: RepartitionExec for hash-based distribution
pub struct RepartitionExec {
    input: Arc<dyn ExecutionPlan>,
    partitioning: Partitioning::Hash(vec![...], num_partitions),
}
```

**Key Design:** Exchange operators use **Arrow Flight for data transfer**:
- Request: `DoGet(Ticket)` — worker requests data from another worker
- Response: `stream FlightData` — streaming RecordBatch transfer
- Transport: gRPC with Arrow memory format (zero-copy where possible)

**Component Boundary:** Exchange operators define **stage boundaries**. All operators between two Exchanges form a stage that runs entirely on a single worker (for that partition).

### 5. Scheduler

**Responsibility:** Assign tasks to workers based on data locality and load.

| Function | Description |
|----------|-------------|
| Split Assignment | Match tasks to workers that have local data |
| Load Balancing | Distribute work evenly across workers |
| Task Retry | Re-assign failed tasks to alternative workers |
| Work Stealing | Idle workers steal tasks from busy workers |

**Key Design:** The scheduler operates on **splits** (partition references), not raw data. Workers register available splits; scheduler assigns.

```
Coordinator Scheduler
     │
     ├──▶ Worker A: [split_0, split_1, split_5]  (local)
     ├──▶ Worker B: [split_2, split_3]           (local)
     ├──▶ Worker C: [split_4]                    (local)
     │
     └──▶ All workers receive RemoteSource locations for upstream stages
```

**Component Boundary:** The scheduler owns the **task-to-worker assignment boundary**. It doesn't execute tasks — it decides which worker runs which task.

---

## Component Communication

### Control Plane (gRPC)

Handles coordination messages between coordinator and workers.

| Operation | Direction | Purpose |
|-----------|-----------|---------|
| RegisterWorker | Worker → Coordinator | Announce worker availability |
| CreateTask | Coordinator → Worker | Assign a task with physical plan |
| TaskComplete | Worker → Coordinator | Report task success + stats |
| TaskFailed | Worker → Coordinator | Report failure + error |
| GetTaskStatus | Coordinator → Worker | Query task progress |
| Heartbeat | Worker → Coordinator | Keep-alive + load metrics |

**Protocol:**
```protobuf
service ControlPlane {
  rpc RegisterWorker(RegisterRequest) returns (RegisterResponse);
  rpc CreateTask(CreateTaskRequest) returns (CreateTaskResponse);
  rpc TaskCompleted(TaskCompleteRequest) returns (TaskCompleteResponse);
  rpc TaskFailed(TaskFailedRequest) returns (TaskFailedResponse);
  rpc Heartbeat(HeartbeatRequest) returns (HeartbeatResponse);
}
```

### Data Plane (Arrow Flight)

Handles high-speed data transfer between workers (and coordinator for results).

| Operation | Direction | Purpose |
|-----------|-----------|---------|
| DoGet | Requester → Provider | Fetch partition data |
| DoPut | Provider ← Requester | Upload intermediate results |

**Key Design:** Arrow Flight uses **gRPC streams** with Apache Arrow memory format:
- Zero-copy where possible (Arrow record batches)
- Batched transfer (configurable batch size, ~100K rows default)
- Bidirectional for fault-tolerant execution

```
Worker A (Source)                          Worker B (Consumer)
      │                                          ▲
      │  FlightData (RecordBatch)                │
      │  FlightData (RecordBatch)                │
      │  FlightData (RecordBatch)                │
      │──────────────────────────────────────────┘
```

---

## Data Flow

### Query Lifecycle

```
┌──────────────────────────────────────────────────────────────────┐
│ 1. SQL Submission                                                │
│    Client ──HTTP/REST──▶ Coordinator                             │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ 2. Logical Planning                                              │
│    Coordinator: SQL → AST → Logical Plan                        │
│    (DataFusion SQL crate)                                        │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ 3. Physical Planning                                             │
│    Coordinator: Logical Plan → Physical Plan (ExecutionPlan)    │
│    (DataFusion physical planner)                                 │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ 4. Distributed Planning                                          │
│    Coordinator: Insert Exchange operators, create Stage DAG      │
│    - Stage 0: Scan operators (leaf)                              │
│    - Stage 1: Intermediate operators + Exchange (shuffle)       │
│    - Stage 2: Final aggregation + Exchange (gather)              │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ 5. Task Scheduling                                               │
│    Coordinator ──gRPC CreateTask──▶ Workers                     │
│    Each task includes: physical plan fragment + split assignment │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ 6. Parallel Execution                                            │
│    Workers execute tasks in parallel:                           │
│    - Stage 0 workers scan partitions (Exchange sends to Stage 1)│
│    - Stage 1 workers receive, aggregate, shuffle (Exchange to   │
│      Stage 2)                                                    │
│    - Stage 2 workers receive, final aggregate, send to coord   │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ 7. Result Collection                                              │
│    Coordinator ──Arrow Flight DoGet──▶ Workers (Stage 2)        │
│    Coordinator aggregates and returns to client                 │
└──────────────────────────────────────────────────────────────────┘
```

### Streaming Pipeline Semantics

Unlike batch (Spark) where each stage completes before the next starts, **pipeline execution** keeps all stages running concurrently:

```
Time ──────────────────────────────────────────────────────────────▶

Stage 0:  [Scan part 0] [Scan part 1] [Scan part 2] [Scan part 3]
           └────────────────────────────────────────────────────┘
                              │
                              │ Exchange (stream batches)
                              ▼
Stage 1:    [Agg]────────[Agg]────────[Agg]────────[Agg]
             └───────────────┼────────────────────────────┘
                              │ Exchange (stream batches)
                              ▼
Stage 2:            [Final Agg]────────────────────────
```

- Batches flow through immediately (no waiting for full stage completion)
- Backpressure propagates upstream (slow consumers slow producers)
- Memory bounded by pipeline depth, not total data size

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENT                                         │
│                          (JDBC/CLI)                                      │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │ HTTP / REST
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          COORDINATOR                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │   Parser     │  │   Planner    │  │  Scheduler  │  │  Metadata   │  │
│  │  (SQL→AST)   │  │  (Logical    │  │  (Task      │  │   Store     │  │
│  │              │  │   +Physical) │  │   Assign)   │  │  (Catalog)  │  │
│  └─────────────┘  └──────────────┘  └─────────────┘  └─────────────┘  │
│         │               │                   │                 │         │
│         │               │                   │                 │         │
│         │               ▼                   │                 │         │
│         │      ┌──────────────────┐         │                 │         │
│         │      │  Stage DAG Plan   │         │                 │         │
│         │      │  (with Exchanges) │         │                 │         │
│         │      └──────────────────┘         │                 │         │
│         │               │                   │                 │         │
│         └───────────────┼───────────────────┼─────────────────┘         │
│                         │ gRPC (Control)                                 │
└─────────────────────────┼───────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│   WORKER 1     │ │   WORKER 2    │ │   WORKER N    │
│  ┌───────────┐ │  ┌───────────┐ │  ┌───────────┐ │
│  │Task[0][0] │ │  │Task[0][1] │ │  │Task[0][2] │ │
│  │ (Scan)    │ │  │ (Scan)    │ │  │ (Scan)    │ │
│  └─────┬─────┘ │  └─────┬─────┘ │  └─────┬─────┘ │
│        │             │             │         │
│        │  Arrow Flight (Data)      │         │
│        └─────────────┼─────────────┘         │
│                      │ Exchange              │
│                      ▼                       │
│               ┌─────────────┐                │
│               │Task[1][0]   │◀─── RemoteSource
│               │ (Aggregate)│                │
│               └──────┬──────┘                │
│                      │                       │
│              Arrow Flight (Data)             │
│                      │                       │
│                      ▼                       │
│               ┌─────────────┐                │
│               │Task[2][0]   │◀─── RemoteSource
│               │(Final Agg)  │                │
│               └──────┬──────┘                │
│                      │                       │
│              Arrow Flight (Results)          │
└──────────────────────┼───────────────────────────────────────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │  COORDINATOR    │
              │ (Result Aggr.)  │
              └─────────────────┘
```

---

## Stage Boundaries and Exchange Points

**Key Rule:** Exchange operators define stage boundaries. All operators **between** Exchange operators execute locally on a worker without network transfer.

```
Physical Plan with Exchanges:

┌─────────────────────────────────────────────────────────────────┐
│ RemoteSource[2]  ◀── Exchange boundary (receives from Stage 2)│
│        │                                                        │
│        ▼                                                        │
│ ┌─────────────┐                                                 │
│ │ AggregateExec │ ◀── Runs locally on this worker             │
│ │ (Final)       │    No network I/O                            │
│ └──────┬──────┘                                                 │
│        │                                                        │
│        ▼                                                        │
│ ┌─────────────┐                                                 │
│ │LocalExchange│ ◀── Redistribute within worker (hash/single)   │
│ └──────┬──────┘                                                 │
│        │                                                        │
│        ▼                                                        │
│ ┌─────────────┐                                                 │
│ │ AggregateExec │ ◀── Runs locally                              │
│ │ (Partial)     │                                               │
│ └──────┬──────┘                                                 │
│        │                                                        │
│        ▼                                                        │
│ RemoteSink[1]  ◀── Exchange boundary (sends to Stage 1)       │
└─────────────────────────────────────────────────────────────────┘

Stage 1 (this fragment):
  - Receives from Stage 2 via RemoteSource
  - Runs Aggregate (Final) locally
  - Runs LocalExchange (no network)
  - Runs Aggregate (Partial) locally
  - Sends to Stage 1 via RemoteSink
```

---

## Build Order Implications

### Phase 1: Single-Node DataFusion Foundation

**Goal:** Get DataFusion executing queries locally.

| Component | Build First | Rationale |
|-----------|-------------|-----------|
| SQL Parser | Required | All query processing starts here |
| Logical Planner | Required | DataFusion provides this |
| Physical Planner | Required | DataFusion provides this |
| ExecutionPlan trait impl | Required | All operators implement this |
| Basic operators | Required | Scan, Filter, Project, Aggregate |

**Dependencies:** None (greenfield)
**Test:** Run SQL queries against local Parquet/CSV files

### Phase 2: Coordinator Core

**Goal:** Coordinator that parses SQL and creates distributed plans.

| Component | Build Second | Rationale |
|-----------|-------------|-----------|
| Coordinator service | After single-node | Needs query planning first |
| gRPC control plane | After Coordinator | Communication backbone |
| Worker registration | After control plane | Workers need to connect |
| Task creation/distribution | After registration | Core coordinator function |
| Stage DAG planning | After task creation | Inserts Exchange operators |

**Dependencies:** Phase 1 (DataFusion execution)
**Test:** Coordinator creates task assignments for simple queries

### Phase 3: Worker Execution

**Goal:** Workers that execute assigned tasks.

| Component | Build Third | Rationale |
|-----------|-------------|-----------|
| Worker service | After control plane | Receives tasks via gRPC |
| Task execution | After worker service | Executes physical plans |
| Arrow Flight data plane | After task execution | Exchange operator transport |
| Exchange operator (send) | After Flight | Workers send to each other |
| Exchange operator (recv) | After send | Workers receive from each other |

**Dependencies:** Phase 1 (ExecutionPlan), Phase 2 (coordinator + control plane)
**Test:** Simple 2-stage query: Scan → Aggregate (one Exchange)

### Phase 4: Distributed Optimization

**Goal:** Intelligent scheduling and query optimization.

| Component | Build Fourth | Rationale |
|-----------|--------------|-----------|
| Partition metadata | After basic execution | Enables locality-aware scheduling |
| Data locality scheduler | After metadata | Assign tasks to workers with local data |
| Predicate pushdown | After scheduling | Reduce data movement |
| Exchange optimization | After pushdown | Minimize shuffle volume |

**Dependencies:** Phases 1-3 (basic execution working)
**Test:** Verify partition pruning reduces network transfer

### Phase 5: Fault Tolerance & HA

**Goal:** Handle failures gracefully.

| Component | Build Fifth | Rationale |
|-----------|-------------|-----------|
| Task retry | After basic execution | Fast-fail with retry |
| Heartbeat + monitoring | After task retry | Detect failures |
| Multi-coordinator | After monitoring | HA coordinator |
| Work stealing | After monitoring | Handle load imbalance |

**Dependencies:** Phases 1-4 (execution working)
**Test:** Kill worker mid-query, verify retry

---

## Component Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│                        BUILD DEPENDENCIES                           │
└─────────────────────────────────────────────────────────────────────┘

[SQL Parser] ─────┬───▶ [Logical Planner] ────▶ [Physical Planner]
                  │           │                        │
                  │           │                        │
                  │           ▼                        │
                  │    [Distributed Planner] ◀─────────┘
                  │           │                         (Octopus Extension)
                  │           │
                  │           ▼
                  │    [Stage DAG with Exchanges]
                  │           │
                  │           ▼
                  │    [Task Scheduler] ────▶ [Coordinator]
                  │           │                        ▲
                  │           │                        │
                  │           ▼                        │
                  │    [gRPC Control Plane] ──────────┘
                  │           │
                  │           │
                  │           ▼
                  └──────┬──────────────┐
                         │              │
                         ▼              ▼
               ┌──────────────┐  ┌──────────────┐
               │ Worker 1     │  │ Worker N     │
               │ ┌──────────┐ │  │ ┌──────────┐ │
               │ │Task Exec │ │  │ │Task Exec │ │
               │ └────┬─────┘ │  │ └────┬─────┘ │
               │      │       │  │      │       │
               │      ▼       │  │      ▼       │
               │ ┌──────────┐ │  │ ┌──────────┐ │
               │ │  Arrow   │ │  │ │  Arrow   │ │
               │ │  Flight  │ │  │ │  Flight  │ │
               │ └────┬─────┘ │  │ └────┬─────┘ │
               └──────┼───────┘  └──────┼───────┘
                      │                  │
                      └────────┬─────────┘
                               │ (Arrow Flight Data Plane)
                               ▼
                    ┌─────────────────────┐
                    │   Exchange Operator │
                    │  (Repartition/Gather)│
                    └─────────────────────┘
```

---

## Key Architectural Decisions

| Decision | Rationale | Consequence |
|----------|-----------|-------------|
| Exchange operators in ExecutionPlan | DataFusion's extension points allow this | Pipeline semantics preserved |
| Arrow Flight for data transfer | Columnar, zero-copy, gRPC streaming | High throughput, complex dependency |
| gRPC for control plane | Mature, multiplexed, bidirectional | Single port for all coordination |
| Stages as ExecutionPlan fragments | Each stage is a DataFusion subgraph | Reuse DataFusion operators |
| Coordinator owns all planning | Workers are stateless executors | Simplifies failure handling |
| Streaming over batch | Trino-style, no stage materialization | Low latency, memory-bounded |

---

## Sources

- [Trino Architecture (Official)](https://trino.io/docs/current/overview/concepts) — HIGH confidence
- [DataFusion Architecture (Official)](https://github.com/apache/datafusion/blob/main/docs/source/library-user-guide/architecture.md) — HIGH confidence
- [DataFusion Physical Plan (Official)](https://github.com/apache/datafusion/blob/main/docs/source/user-guide/explain-usage.md) — HIGH confidence
- [Arrow Flight Protocol (Official)](https://github.com/apache/arrow/blob/main/docs/source/format/Flight.md) — HIGH confidence
- [Apache Spark Exchange (Official)](https://spark.apache.org/docs/latest/sql-performance-tuning.html) — HIGH confidence
- [Flink Runtime Architecture (Official)](https://github.com/apache/flink/blob/master/docs/content/docs/concepts/flink-architecture.md) — MEDIUM confidence (streaming-first differs from MPP batch)
