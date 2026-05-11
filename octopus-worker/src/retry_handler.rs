//! Retry handler for task execution with configurable retry policy.
//!
//! Per D-04: Failed tasks retry on the same worker before rescheduling.
//! Implements exponential backoff with jitter to avoid thundering herd.

use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::time::sleep;
use tracing::{info, warn, error};
use crate::task_processor::TaskProcessor;
use crate::metrics::MetricsCollector;
use octopus_common::{Result, OctopusError};

/// Configuration for retry behavior.
#[derive(Debug, Clone)]
pub struct RetryConfig {
    /// Maximum number of retry attempts before giving up
    pub max_retries: u32,
    /// Initial delay between retries (milliseconds)
    pub initial_delay_ms: u64,
    /// Multiplier for exponential backoff
    pub backoff_multiplier: f64,
    /// Maximum delay between retries (milliseconds)
    pub max_delay_ms: u64,
    /// Jitter factor (0.0 to 1.0) for randomization
    pub jitter: f64,
}

impl Default for RetryConfig {
    fn default() -> Self {
        Self {
            max_retries: 3,
            initial_delay_ms: 100,
            backoff_multiplier: 2.0,
            max_delay_ms: 5000,
            jitter: 0.1,
        }
    }
}

impl RetryConfig {
    pub fn new(max_retries: u32) -> Self {
        Self {
            max_retries,
            ..Default::default()
        }
    }

    /// Calculate delay for a given attempt number with exponential backoff and jitter.
    fn calculate_delay(&self, attempt: u32) -> Duration {
        let base_delay = self.initial_delay_ms as f64
            * self.backoff_multiplier.powi(attempt as i32);

        let delay = base_delay.min(self.max_delay_ms as f64);

        // Add jitter to avoid thundering herd
        let jitter_range = delay * self.jitter;
        let jitter = (random_f64() * 2.0 - 1.0) * jitter_range;

        Duration::from_millis((delay + jitter).max(0.0) as u64)
    }
}

/// Result of a task execution attempt.
#[derive(Debug)]
pub enum TaskResult {
    Success(String),
    RetryableError(String),
    FatalError(String),
}

/// Handles task execution with retry logic.
pub struct RetryHandler {
    processor: Arc<TaskProcessor>,
    metrics: Arc<MetricsCollector>,
    config: RetryConfig,
    worker_id: String,
}

impl RetryHandler {
    pub fn new(
        processor: Arc<TaskProcessor>,
        metrics: Arc<MetricsCollector>,
        config: RetryConfig,
        worker_id: String,
    ) -> Self {
        Self {
            processor,
            metrics,
            config,
            worker_id,
        }
    }

    /// Execute a task with retry logic.
    /// Returns the result after exhausting retries or on success.
    pub async fn execute_with_retry(
        &self,
        task_id: String,
        plan_json: String,
    ) -> Result<String> {
        let mut attempts = 0;
        let mut last_error = None;

        loop {
            attempts += 1;

            info!("Executing task {} (attempt {}/{})",
                  task_id, attempts, self.config.max_retries + 1);

            // Attempt execution
            let start = Instant::now();
            let result = self.processor.execute_plan_json(&plan_json).await;
            let duration = start.elapsed();

            match result {
                Ok(result_str) => {
                    // Task succeeded
                    self.metrics.record_task_duration(&task_id, duration);
                    self.metrics.record_task_success(&task_id, attempts);
                    return Ok(result_str);
                }
                Err(e) => {
                    let error_str = e.to_string();
                    let is_retryable = Self::is_retryable_error(&error_str);

                    if is_retryable {
                        warn!("Task {} failed (attempt {}): {}", task_id, attempts, error_str);
                        last_error = Some(error_str);

                        if attempts > self.config.max_retries {
                            error!("Task {} exhausted retries ({} attempts)",
                                   task_id, attempts);
                            self.metrics.record_task_failure(&task_id, &last_error.clone().unwrap());
                            return Err(OctopusError::ExecutionError(
                                format!("Task {} failed after {} attempts: {}",
                                       task_id, attempts, last_error.unwrap())
                            ));
                        }

                        let delay = self.config.calculate_delay(attempts - 1);
                        info!("Retrying task {} in {:?}", task_id, delay);
                        sleep(delay).await;
                    } else {
                        // Fatal error - don't retry
                        error!("Task {} had fatal error: {}", task_id, error_str);
                        self.metrics.record_task_failure(&task_id, &error_str);
                        return Err(OctopusError::ExecutionError(error_str));
                    }
                }
            }
        }
    }

    /// Classify an error as retryable or fatal.
    fn is_retryable_error(error: &str) -> bool {
        // Retryable errors: transient issues that might succeed on retry
        // - Network timeouts
        // - Resource contention
        // - Temporary unavailability

        // Fatal errors: permanent issues that won't be fixed by retry
        // - Invalid query plan
        // - Type mismatch
        // - Schema not found

        let retryable_patterns = [
            "timeout",
            "connection refused",
            "temporarily unavailable",
            "resource contention",
            "memory pressure",
        ];

        let fatal_patterns = [
            "invalid plan",
            "schema not found",
            "type mismatch",
            "parse error",
            "column not found",
        ];

        // Check for fatal patterns first
        for pattern in fatal_patterns {
            if error.to_lowercase().contains(pattern) {
                return false;
            }
        }

        // Check for retryable patterns
        for pattern in retryable_patterns {
            if error.to_lowercase().contains(pattern) {
                return true;
            }
        }

        // Default to retryable for unknown errors
        true
    }
}

/// Task execution result with retry metadata.
#[derive(Debug)]
pub struct RetryResult {
    pub task_id: String,
    pub success: bool,
    pub attempts: u32,
    pub total_duration_ms: u64,
    pub error: Option<String>,
}

impl RetryResult {
    pub fn success(task_id: String, attempts: u32, duration_ms: u64) -> Self {
        Self {
            task_id,
            success: true,
            attempts,
            total_duration_ms: duration_ms,
            error: None,
        }
    }

    pub fn failure(task_id: String, attempts: u32, duration_ms: u64, error: String) -> Self {
        Self {
            task_id,
            success: false,
            attempts,
            total_duration_ms: duration_ms,
            error: Some(error),
        }
    }
}

// Simple random for jitter using thread-local state
thread_local! {
    static RNG_STATE: std::cell::Cell<u64> = std::cell::Cell::new(0);
}

fn random_f64() -> f64 {
    RNG_STATE.with(|state| {
        let current = state.get();
        // Simple hash of current time and thread ID for randomness
        let new = current.wrapping_mul(1103515245).wrapping_add(12345);
        state.set(new);
        // Convert to f64 in range [0, 1)
        (new % 1000000) as f64 / 1000000.0
    })
}