# Technology Stack

**Project:** Octopus — Distributed MPP Query Engine
**Researched:** 2026-04-22
**Overall confidence:** HIGH

## Executive Summary

Octopus builds on Apache DataFusion with a clean separation between control plane (gRPC via Tonic) and data plane (Arrow Flight). The stack leverages Rust's async ecosystem: Tokio as the runtime, tonic-prost for gRPC code generation, and trait-based plugin architecture for metadata storage. This mirrors Ballista's proven architecture while targeting streaming/pipeline execution instead of batch.

---

## Recommended Stack

### Core Runtime

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **Tokio** | 1.52 | Async runtime | Industry standard for Rust async. Multi-threaded runtime with `rt_multi_thread` for worker tasks and `spawn_blocking` for CPU-intensive work. DataFusion itself uses Tokio. |
| `async-trait` | 0.1.89 | Async trait support | Rust requires async fn in traits to be boxed or use this macro. Essential for trait-based plugin architectures (e.g., TableProvider, MetadataStore). |

**Confidence: HIGH** — Version verified against DataFusion 53 workspace dependencies.

---

### Control Plane: gRPC / RPC

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **tonic** | 0.14 | gRPC framework | Native Rust gRPC over HTTP/2. First-class async/await support. Built on hyper. Used by arrow-flight and Ballista. |
| **prost** | 0.14 | Protocol Buffers | The standard protobuf implementation for Rust. Generates idiomatic Rust from `.proto` files. Used by tonic and arrow-flight. |
| `tonic-build` | 0.14 | Code generation | `prost` + `tonic` code gen from `.proto` files. Use `include_proto!` macro to import generated modules. |
| `tonic-prost` | 0.14 | Tonic-Prost interop | Bridges tonic `Request`/`Response` with prost message types. |

**Confidence: HIGH** — Verified against DataFusion/Ballista Cargo.toml workspace dependencies. tonic 0.14.x is current stable.

**Anti-patterns to avoid:**
- **BloomRPC/Postman for gRPC** — debugging tools only, not for production
- **grpcio** (grpcio-rs) — abandoned/deprecated; use tonic instead
- **avrpc** — not production-ready

---

### Data Plane: Arrow Flight

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **arrow-flight** | 58.1.0 | High-speed data transfer | Apache Arrow's RPC framework for transferring Arrow record batches. Built on tonic. Zero-copy data transfer. |
| **arrow** | 58.1.0 | Arrow memory format | Core Arrow columnar format. Use `arrow-array`, `arrow-schema`, `arrow-ipc` as needed. |
| `arrow-buffer` | 58.1.0 | Arrow memory layout | Low-level buffer types for Arrow arrays. |
| `arrow-schema` | 58.1.0 | Arrow schema definition | Schema, Field, DataType types. |

**Confidence: HIGH** — Apache Arrow is the standard. Version 58.x matches DataFusion 53's Arrow version.

**Why Arrow Flight instead of custom binary protocol?**
- Arrow Flight is designed specifically for columnar data movement
- Supports compression (lz4 via arrow-ipc with `ipc_compression` feature)
- Built-in Flight SQL protocol for query execution
- Native Rust implementation in arrow-rs

---

### Query Execution Engine

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **datafusion** | 53.x | Query engine | Apache DataFusion is the only production-grade extensible query engine in Rust. Provides SQL parsing, optimization, and execution. |
| `datafusion-proto` | 53.x | Physical plan serialization | For distributed execution, need to serialize/deserialize physical plans between coordinator and workers. |
| `datafusion-execution` | 53.x | Execution runtime | Provides TaskContext, ExecutionConfig, memory management. |

**Confidence: HIGH** — Apache DataFusion is the de facto standard for Rust OLAP engines. Ballista (DataFusion's distributed extension) uses the same stack.

**Key extension points for Octopus:**
- `OptimizerRule` — add distributed optimization rules
- `PhysicalOptimizer` — partition-aware physical planning
- `ExecutionPlan` — implement Exchange operator for streaming

---

### Database / Metadata Storage (Pluggable)

The PROJECT.md specifies a trait-based pluggable metadata store. Recommended implementations:

| Implementation | Use Case | Library | Confidence |
|----------------|----------|---------|------------|
| **PostgreSQL** | Production metadata | `sqlx` 0.8+ with `postgres` feature | HIGH |
| **SQLite** | Single-node / dev | `sqlx` 0.8+ with `sqlite` feature | HIGH |
| **etcd** | Distributed coordination | `rust-etcd` or custom gRPC | MEDIUM — rust-etcd (0.9.0) is outdated; consider native etcd client or Ballista's in-memory + external approach |
| **In-memory** | Ephemeral / testing | Built-in `HashMap`/`DashMap` | HIGH |

**sqlx version:** 0.8.x (from DataFusion 53's indirect dependencies)
**Why sqlx over raw drivers?** Compile-time checked queries via `query!` / `query_as!` macros. Async support. TLS. Connection pooling.

**Metadata store trait design (recommended):**
```rust
#[async_trait]
pub trait MetadataStore: Send + Sync {
    async fn get_table(&self, name: &str) -> Result<Option<TableMeta>>;
    async fn list_tables(&self) -> Result<Vec<TableMeta>>;
    async fn create_table(&self, meta: TableMeta) -> Result<()>;
    async fn update_table(&self, meta: TableMeta) -> Result<()>;
    async fn delete_table(&self, name: &str) -> Result<()>;
    // For job state, partition locations, etc.
    async fn get_job_state(&self, job_id: &str) -> Result<Option<JobState>>;
    async fn update_job_state(&self, state: JobState) -> Result<()>;
}
```

**Confidence: MEDIUM for etcd** — The `rust-etcd` crate (0.9.0) hasn't been updated for modern Tokio. For production etcd coordination, consider either:
1. Using `sled` or `rocksdb` for local state + etcd for coordination separately
2. Custom lightweight gRPC client using tonic for etcd's v3 API
3. External service approach (coordinator writes to etcd, workers read via etcd client)

---

### Object Storage

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **object_store** | 0.13.x | Unified object storage | S3, Azure Blob, GCS, HDFS, local files via统一的 API. Used by datafusion-parquet. |
| **parquet** | 58.1.0 | Parquet format | Read/write Parquet files. Enable `async`, `object_store` features. |

**Confidence: HIGH** — object_store is the standard Rust library for cloud object storage. Used by DataFusion.

---

### Observability

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **tracing** | 0.1 | Structured logging + spans | The standard for Rust async observability. Used by tokio, tonic, datafusion. |
| **tracing-subscriber** | 0.3 | Subscriber implementation | Env-filter, JSON formatting, async-friendly. |
| `tracing-appender` | 0.2 | Non-blocking log writer | Write logs to file without blocking the async runtime. |
| `opentelemetry` + `opentelemetry-otlp` | latest | Distributed tracing | Export to Jaeger, Tempo, etc. Optional for v1. |
| `metrics` + `prometheus` | latest | Metrics | For `/metrics` endpoint. Optional for v1. |

**Confidence: MEDIUM** — OpenTelemetry ecosystem is rapidly evolving in Rust. For v1, start with tracing + structured JSON logs.

---

### Serialization

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **serde** + `serde_json` | 1.0 | JSON serialization | Standard Rust serialization. Used for config, HTTP payloads. |
| `prost-types` | 0.14 | Protobuf well-known types | For Timestamp, Duration, etc. in gRPC messages. |

**Confidence: HIGH** — Industry standard.

---

### CLI / Config

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **clap** | 4.5 | CLI argument parsing | Derive-based CLI. Used by Ballista. |
| **tokio** with `full` feature | 1.52 | Task scheduling | For executor binary. Coordinator can use `net` feature only. |
| `config` or env vars | — | Configuration | No heavy config library needed for v1. Use env vars + clap. |

**Confidence: HIGH** — Simple stack for prototype phase.

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Async runtime | Tokio | async-std | Tokio has better ecosystem, used by DataFusion/Ballista, more mature |
| gRPC framework | tonic | grpcio | grpcio is effectively abandoned; tonic is the active standard |
| Metadata | Trait-based pluggable | Fixed backend | Octopus requirements explicitly call for pluggable; Ballista uses in-memory |
| SQL engine | DataFusion | Custom parser | DataFusion is production-grade; reinventing is waste |
| Object storage | object_store | Custom S3/HDFS clients | object_store unifies S3/HDFS/Azure/GCS with one API |
| Distributed consensus | External (etcd/Raft) | Built-in Raft | Use established tools; `raft-rs` (TikV) is complex. Octopus "Multi-Coordinator" note says "external service or self-hosted Raft" |

---

## Quick-Reference Cargo Snippet

```toml
[dependencies]
# Core execution
datafusion = "53"
datafusion-proto = "53"
datafusion-execution = "53"

# Arrow ecosystem
arrow = "58"
arrow-flight = "58"  # includes tonic prost interop
arrow-schema = "58"
arrow-array = "58"
arrow-ipc = "58"

# gRPC / RPC
tonic = "0.14"
prost = "0.14"
tonic-build = "0.14"
tonic-prost = "0.14"

# Async runtime
tokio = { version = "1.52", features = ["rt-multi-thread", "macros", "sync"] }
async-trait = "0.1"

# Object storage
object_store = "0.13"
parquet = "58"

# Metadata / persistence
sqlx = { version = "0.8", features = ["runtime-tokio", "postgres", "sqlite"] }

# Observability
tracing = "0.1"
tracing-subscriber = { version = "0.3", features = ["env-filter", "json"] }
tracing-appender = "0.2"

# Serialization
serde = "1"
serde_json = "1"

# CLI
clap = { version = "4.5", features = ["derive"] }

[dev-dependencies]
tempfile = "3"
tokio-test = "0.4"
```

---

## What NOT to Use and Why

| Library | Why Avoid |
|---------|-----------|
| `grpcio` | Deprecated/abandoned. Use `tonic`. |
| `rust-etcd` (0.9.x) | Uses outdated tokio 0.1. If needing etcd, implement custom tonic client or use `sled` + external etcd. |
| `actix-web` | Web framework, not needed for internal RPC. Use tonic directly. |
| ` Diesel` | Synchronous ORM. Use `sqlx` for async. |
| `collenchyma` | Deprecated. Use `arrow` kernels directly. |
| `immortale` | Unmaintained Arrow IPC. Use `arrow-ipc`. |

---

## Sources

- **DataFusion 53 Cargo.toml**: https://raw.githubusercontent.com/apache/datafusion/refs/heads/main/Cargo.toml (workspace dependencies, verified Arrow 58, tonic 0.14, prost 0.14, tokio 1.52)
- **Ballista Cargo.toml**: https://raw.githubusercontent.com/apache/datafusion-ballista/main/Cargo.toml (same stack, scheduler + executor configs)
- **arrow-flight crate**: https://docs.rs/arrow-flight/58.1.0 (tonic integration, flight-sql)
- **tonic crate**: https://docs.rs/tonic/0.14.5 (gRPC framework, feature flags, TLS)
- **prost crate**: https://docs.rs/prost/0.14.3 (protobuf code generation)
- **Ballista architecture**: https://github.com/apache/arrow-datafusion (Ballista is DataFusion's distributed subproject)
