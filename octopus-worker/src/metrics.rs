//! Metrics collection for worker observability.
//!
//! Per OBS-02: System emits metrics (CPU, memory, rows processed per stage).
//! Metrics are collected continuously during task execution and emitted
//! to the coordinator for aggregation.

use std::sync::Arc;
use std::sync::atomic::{AtomicU64, AtomicU32, Ordering};
use std::time::{Duration, Instant};
use tracing::{info, warn};

/// Metrics for a single task.
#[derive(Debug, Clone)]
pub struct TaskMetrics {
    pub task_id: String,
    pub rows_processed: u64,
    pub cpu_percent: f64,
    pub memory_bytes: u64,
    pub duration_ms: u64,
    pub attempts: u32,
}

/// Worker-level aggregated metrics.
#[derive(Debug, Clone)]
pub struct WorkerMetrics {
    pub worker_id: String,
    pub cpu_percent: f64,
    pub memory_bytes: u64,
    pub rows_processed_total: u64,
    pub active_tasks: u32,
    pub timestamp: i64,
}

impl Default for WorkerMetrics {
    fn default() -> Self {
        Self {
            worker_id: String::new(),
            cpu_percent: 0.0,
            memory_bytes: 0,
            rows_processed_total: 0,
            active_tasks: 0,
            timestamp: 0,
        }
    }
}

/// Metrics collector for worker observability.
pub struct MetricsCollector {
    worker_id: String,
    /// Current CPU usage (updated by system monitor)
    cpu_percent: AtomicU64,
    /// Current memory usage (updated by system monitor)
    memory_bytes: AtomicU64,
    /// Total rows processed across all tasks
    rows_processed: AtomicU64,
    /// Currently active tasks
    active_tasks: AtomicU32,
    /// Metrics emission interval
    emission_interval: Duration,
}

impl MetricsCollector {
    pub fn new(worker_id: String) -> Self {
        Self {
            worker_id,
            cpu_percent: AtomicU64::new(0),
            memory_bytes: AtomicU64::new(0),
            rows_processed: AtomicU64::new(0),
            active_tasks: AtomicU32::new(0),
            emission_interval: Duration::from_secs(5),
        }
    }

    /// Update CPU usage percentage.
    pub fn update_cpu(&self, percent: f64) {
        self.cpu_percent.store((percent * 100.0) as u64, Ordering::Relaxed);
    }

    /// Update memory usage in bytes.
    pub fn update_memory(&self, bytes: u64) {
        self.memory_bytes.store(bytes, Ordering::Relaxed);
    }

    /// Record rows processed for a task.
    pub fn record_rows(&self, task_id: &str, rows: u64) {
        self.rows_processed.fetch_add(rows, Ordering::Relaxed);
        let _ = task_id; // Suppress unused warning in release mode
    }

    /// Increment active task count.
    pub fn task_started(&self, _task_id: &str) {
        self.active_tasks.fetch_add(1, Ordering::Relaxed);
    }

    /// Decrement active task count.
    pub fn task_completed(&self, _task_id: &str) {
        self.active_tasks.fetch_sub(1, Ordering::Relaxed);
    }

    /// Record task success.
    pub fn record_task_success(&self, task_id: &str, attempts: u32) {
        info!("Task {} completed successfully after {} attempt(s)", task_id, attempts);
        self.task_completed(task_id);
    }

    /// Record task failure.
    pub fn record_task_failure(&self, task_id: &str, error: &str) {
        warn!("Task {} failed: {}", task_id, error);
        self.task_completed(task_id);
    }

    /// Record task duration.
    pub fn record_task_duration(&self, task_id: &str, duration: Duration) {
        let duration_ms = duration.as_millis() as u64;
        let _ = task_id;
        let _ = duration_ms;
    }

    /// Get current worker metrics.
    pub fn get_metrics(&self) -> WorkerMetrics {
        WorkerMetrics {
            worker_id: self.worker_id.clone(),
            cpu_percent: self.cpu_percent.load(Ordering::Relaxed) as f64 / 100.0,
            memory_bytes: self.memory_bytes.load(Ordering::Relaxed),
            rows_processed_total: self.rows_processed.load(Ordering::Relaxed),
            active_tasks: self.active_tasks.load(Ordering::Relaxed),
            timestamp: Instant::now().elapsed().as_millis() as i64,
        }
    }

    /// Emit metrics (for integration with observability system).
    pub fn emit_metrics(&self) -> WorkerMetrics {
        let metrics = self.get_metrics();
        info!("Worker metrics: CPU={:.1}%, Memory={}MB, Rows={}, ActiveTasks={}",
              metrics.cpu_percent,
              metrics.memory_bytes / 1024 / 1024,
              metrics.rows_processed_total,
              metrics.active_tasks);
        metrics
    }
}

/// System monitor for collecting CPU and memory metrics.
pub struct SystemMonitor {
    metrics: Arc<MetricsCollector>,
}

impl SystemMonitor {
    pub fn new(metrics: Arc<MetricsCollector>) -> Self {
        Self { metrics }
    }

    /// Start monitoring system metrics.
    pub async fn start(&self) {
        info!("Starting system metrics monitor");

        loop {
            // Collect CPU and memory metrics
            self.update_system_metrics();

            // Sleep for the collection interval
            tokio::time::sleep(Duration::from_secs(1)).await;
        }
    }

    fn update_system_metrics(&self) {
        // In production, use sysinfo crate or similar for accurate metrics
        // For now, use placeholder values

        // CPU: Could use sysinfo::Cpu.refresh()
        let cpu = self.get_cpu_usage();
        self.metrics.update_cpu(cpu);

        // Memory: Could use sysinfo::System.refresh_memory()
        let memory = self.get_memory_usage();
        self.metrics.update_memory(memory);
    }

    fn get_cpu_usage(&self) -> f64 {
        // Placeholder: In production, use sysinfo or /proc/stat
        0.0
    }

    fn get_memory_usage(&self) -> u64 {
        // Placeholder: In production, use sysinfo or /proc/meminfo
        0
    }
}