//! Task processor for executing DataFusion physical plans.
//!
//! Executes compute-intensive tasks on the dedicated CPU thread pool,
//! separate from the async IO runtime.

use std::sync::Arc;
use datafusion::physical_plan::ExecutionPlan;
use datafusion::execution::runtime_env::{RuntimeConfig, RuntimeEnv};
use datafusion::prelude::SessionContext;
use crate::runtime::CpuRuntime;
use crate::retry_handler::{RetryHandler, RetryConfig};
use crate::metrics::MetricsCollector;
use octopus_common::{Result, OctopusError};

/// Executes physical plans on the CPU thread pool.
pub struct TaskProcessor {
    cpu_runtime: Arc<CpuRuntime>,
    session_context: SessionContext,
    retry_handler: Option<Arc<RetryHandler>>,
    metrics: Arc<MetricsCollector>,
}

impl TaskProcessor {
    pub fn new(cpu_runtime: Arc<CpuRuntime>) -> Result<Self> {
        // Create DataFusion SessionContext for query execution
        let config = datafusion::prelude::SessionConfig::new()
            .with_target_partitions(num_cpus::get())
            .with_information_schema(true);

        let runtime = Arc::new(
            RuntimeEnv::try_new(RuntimeConfig::default())
                .map_err(|e| OctopusError::ExecutionError(e.to_string()))?
        );

        let session_context = SessionContext::new_with_config_rt(config, runtime);

        tracing::info!("TaskProcessor initialized with CPU thread pool");

        Ok(Self {
            cpu_runtime,
            session_context,
            retry_handler: None,
            metrics: Arc::new(MetricsCollector::new("default".to_string())),
        })
    }

    /// Create a TaskProcessor with retry support.
    pub fn with_retry(
        cpu_runtime: Arc<CpuRuntime>,
        retry_config: RetryConfig,
        metrics: Arc<MetricsCollector>,
        worker_id: String,
    ) -> Result<Self> {
        let session_context = {
            let config = datafusion::prelude::SessionConfig::new()
                .with_target_partitions(num_cpus::get())
                .with_information_schema(true);

            let runtime = Arc::new(
                RuntimeEnv::try_new(RuntimeConfig::default())
                    .map_err(|e| OctopusError::ExecutionError(e.to_string()))?
            );

            SessionContext::new_with_config_rt(config, runtime)
        };

        // Create the base processor
        let processor = Arc::new(Self {
            cpu_runtime: cpu_runtime.clone(),
            session_context,
            retry_handler: None,
            metrics: metrics.clone(),
        });

        // Create retry handler
        let retry_handler = Some(Arc::new(RetryHandler::new(
            processor.clone(),
            metrics.clone(),
            retry_config,
            worker_id,
        )));

        Ok(Self {
            cpu_runtime,
            session_context: processor.session_context.clone(),
            retry_handler,
            metrics,
        })
    }

    /// Execute plan with retry support.
    pub async fn execute_plan_with_retry(
        &self,
        task_id: String,
        plan_json: String,
    ) -> Result<String> {
        if let Some(retry_handler) = &self.retry_handler {
            retry_handler.execute_with_retry(task_id, plan_json).await
        } else {
            self.execute_plan_json(&plan_json).await
        }
    }

    /// Execute a physical plan on the CPU thread pool.
    /// Returns the result as a JSON string.
    pub async fn execute_plan(
        &self,
        plan: Arc<dyn ExecutionPlan>,
    ) -> Result<String> {
        let context = self.session_context.clone();

        // Run physical plan execution on CPU thread pool
        let result = self.cpu_runtime.spawn(async move {
            Self::execute_plan_internal(context, plan).await
        }).await
        .map_err(|e| OctopusError::ExecutionError(format!("Task execution failed: {}", e)))??;

        Ok(result)
    }

    /// Internal plan execution (runs on CPU thread pool).
    async fn execute_plan_internal(
        context: SessionContext,
        plan: Arc<dyn ExecutionPlan>,
    ) -> Result<String> {
        use datafusion::physical_plan::execute_stream;
        use futures::StreamExt;

        let task_ctx = context.task_ctx();
        let stream = execute_stream(plan, task_ctx)
            .map_err(|e| OctopusError::ExecutionError(format!("Plan execution error: {}", e)))?;

        let mut batches = Vec::new();
        let mut row_count = 0usize;

        futures::pin_mut!(stream);
        while let Some(batch_result) = stream.next().await {
            let batch = batch_result.map_err(|e|
                OctopusError::ExecutionError(format!("Batch execution error: {}", e)))?;
            row_count += batch.num_rows();
            batches.push(batch);
        }

        tracing::info!("Task completed: {} rows produced", row_count);

        // Convert results to JSON (simplified for now)
        Ok(serde_json::json!({
            "row_count": row_count,
            "batch_count": batches.len(),
        }).to_string())
    }

    /// Execute a physical plan synchronously (blocking).
    pub fn execute_plan_sync(
        &self,
        plan: Arc<dyn ExecutionPlan>,
    ) -> Result<String> {
        let context = self.session_context.clone();

        let handle = tokio::task::spawn_blocking(move || {
            // Use Tokio's block_on for synchronous execution on CPU pool
            let runtime = tokio::runtime::Handle::current();
            runtime.block_on(async move {
                Self::execute_plan_internal(context, plan).await
            })
        });

        // Poll the JoinHandle to get the result
        let rt = tokio::runtime::Handle::current();
        rt.block_on(async {
            handle.await
        }).map_err(|e|
            OctopusError::ExecutionError(format!("Blocking task failed: {}", e)))?
    }

    /// Execute a plan from JSON string.
    /// This is used by RetryHandler for task execution with retry logic.
    pub async fn execute_plan_json(&self, plan_json: &str) -> Result<String> {
        // Parse the JSON to get plan details
        let _plan: serde_json::Value = serde_json::from_str(plan_json)
            .map_err(|e| OctopusError::ExecutionError(format!("Failed to parse plan JSON: {}", e)))?;

        // For now, return a placeholder result indicating plan was "executed"
        // In production, this would deserialize the physical plan and execute it
        let row_count = 0;
        Ok(serde_json::json!({
            "row_count": row_count,
            "status": "completed",
        }).to_string())
    }
}