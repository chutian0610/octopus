# Octopus — GSD Workflow Guide

## Project

Distributed MPP query engine built on Apache DataFusion. Coordinator-worker architecture with streaming pipeline execution. Trino-style, not Spark/Ballista batch.

## Core Value

Users can run fast interactive SQL queries on large distributed datasets with Rust-level performance and memory safety.

## Current Phase

**Phase 1: Single-Node Foundation** — Local SQL execution on Parquet/CSV/JSON with DataFusion

## Workflow

When user says `/gsd-next` or `/gsd-plan-phase 1`:

1. **Read context** — `.planning/PROJECT.md`, current phase in `.planning/ROADMAP.md`, `.planning/STATE.md`
2. **Discuss** — `/gsd-discuss-phase 1 --auto` or manual discussion
3. **Plan** — `/gsd-plan-phase 1` creates `PLANS.md` in phase directory
4. **Execute** — `/gsd-execute-phase 1` runs plans
5. **Verify** — `/gsd-verify-work` confirms deliverables
6. **Commit** — atomic commit with phase artifacts

## Phase Order

1. **Single-Node Foundation** — DataFusion local execution, correct streaming patterns
2. **Coordinator Core** — Distributed query planning, gRPC control plane, CLI
3. **Workers + Arrow Flight** — Parallel execution, Exchange operators, data transfer
4. **Advanced SQL & Federated Sources** — PostgreSQL/MySQL, window functions
5. **Observability & Clients** — Metrics, JDBC driver, UDF support

## Key Constraints

- Rust + DataFusion (no JVM)
- Pipeline streaming (not batch)
- gRPC + Arrow Flight for control/data plane
- Fast-fail fault tolerance with optional task retry

## Reference

- Requirements: `.planning/REQUIREMENTS.md`
- Research: `.planning/research/STACK.md`, `FEATURES.md`, `ARCHITECTURE.md`, `PITFALLS.md`, `SUMMARY.md`
- Roadmap: `.planning/ROADMAP.md`
- State: `.planning/STATE.md`