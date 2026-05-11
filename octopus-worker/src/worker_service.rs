//! Worker service that receives tasks from coordinator and executes them.
//!
//! Follows worker-pull model (D-01): Workers expose services; consumers pull data on demand.
//! Tasks are received via gRPC and executed on the CPU thread pool.

use std::sync::Arc;
use tokio::sync::mpsc;
use uuid::Uuid;
use crate::runtime::WorkerRuntime;
use crate::task_processor::TaskProcessor;
use octopus_common::{Result, OctopusError};

/// Worker service handle for task execution.
pub struct WorkerService {
    worker_id: String,
    runtime: Arc<WorkerRuntime>,
    processor: Arc<TaskProcessor>,
    coordinator_url: String,
}

impl WorkerService {
    /// Create a new worker service.
    pub fn new(coordinator_url: String) -> Result<Self> {
        let runtime = Arc::new(WorkerRuntime::new()?);
        let processor = Arc::new(TaskProcessor::new(runtime.cpu.clone())?);

        let worker_id = Uuid::new_v4().to_string();

        tracing::info!("WorkerService created with ID: {}", worker_id);

        Ok(Self {
            worker_id,
            runtime,
            processor,
            coordinator_url,
        })
    }

    /// Get the worker ID.
    pub fn worker_id(&self) -> &str {
        &self.worker_id
    }

    /// Start the worker's task receiver loop.
    /// This runs on the IO runtime and receives tasks from the coordinator.
    pub async fn run(&self) -> Result<()> {
        tracing::info!("Worker {} starting task receiver loop", self.worker_id);

        // Connect to coordinator for task registration
        self.register_with_coordinator().await?;

        // Task receiver loop (simplified - actual implementation will use gRPC)
        self.task_receiver_loop().await;

        Ok(())
    }

    /// Register this worker with the coordinator.
    async fn register_with_coordinator(&self) -> Result<()> {
        tracing::info!("Registering worker {} with coordinator at {}",
                      self.worker_id, self.coordinator_url);

        // TODO: Use gRPC client to register with coordinator
        // For now, just log the registration

        Ok(())
    }

    /// Main task receiver loop - receives tasks and dispatches to processor.
    async fn task_receiver_loop(&self) {
        tracing::info!("Task receiver loop started for worker {}", self.worker_id);

        // TODO: Replace with actual gRPC task stream from coordinator
        // For now, this is a placeholder that simulates task receiving

        loop {
            // Simulate receiving a task (placeholder)
            tokio::time::sleep(tokio::time::Duration::from_secs(5)).await;

            // In real implementation:
            // - Receive task from coordinator via gRPC stream
            // - Deserialize physical plan
            // - Execute on CPU thread pool
            // - Return results to coordinator or exchange with other workers
        }
    }

    /// Process a single task (used by task receiver).
    async fn process_task(&self, task_id: String, plan_json: String) -> Result<String> {
        tracing::info!("Processing task {} on worker {}", task_id, self.worker_id);

        // TODO: Deserialize physical plan from JSON
        // For now, return a placeholder result

        Ok(serde_json::json!({
            "task_id": task_id,
            "status": "completed",
            "worker_id": self.worker_id,
        }).to_string())
    }

    /// Execute a task synchronously on the CPU thread pool.
    pub fn execute_task_sync(&self, task_id: String, plan_json: String) -> Result<String> {
        let processor = self.processor.clone();

        // Use spawn_blocking on the CPU runtime for synchronous execution
        // Get the runtime handle and spawn a blocking task
        let cpu = self.runtime.cpu.as_ref();
        let handle = cpu.handle().spawn_blocking(move || {
            // Placeholder - actual implementation would deserialize plan and execute
            tracing::info!("Executing task {} synchronously", task_id);
            Ok(serde_json::json!({
                "task_id": task_id,
                "status": "completed",
            }).to_string())
        });

        // Wait for the blocking task to complete
        let rt = tokio::runtime::Handle::current();
        rt.block_on(async {
            handle.await
        }).map_err(|e|
            OctopusError::ExecutionError(format!("Task execution failed: {}", e)))?
    }
}

/// Task message received from coordinator.
#[derive(Debug, Clone)]
pub struct TaskMessage {
    pub task_id: String,
    pub query_id: String,
    pub stage: u32,
    pub partition: u32,
    pub plan_data: Vec<u8>,  // Serialized physical plan
}