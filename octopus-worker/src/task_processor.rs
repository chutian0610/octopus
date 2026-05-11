//! Task processor for executing DataFusion physical plans.
//!
//! Executes compute-intensive tasks on the dedicated CPU thread pool,
//! separate from the async IO runtime.

use std::sync::Arc;
use datafusion::physical_plan::ExecutionPlan;
use datafusion::execution::runtime_env::{RuntimeConfig, RuntimeEnv};
use datafusion::prelude::SessionContext;
use crate::runtime::CpuRuntime;
use octopus_common::{Result, OctopusError};

/// Executes physical plans on the CPU thread pool.
pub struct TaskProcessor {
    cpu_runtime: Arc<CpuRuntime>,
    session_context: SessionContext,
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
        })
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
}