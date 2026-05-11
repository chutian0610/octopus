//! Worker service that receives tasks from coordinator and executes them.
//!
//! Follows worker-pull model (D-01): Workers expose services; consumers pull data on demand.
//! Tasks are received via gRPC and executed on the CPU thread pool.

use std::sync::Arc;
use uuid::Uuid;
use crate::runtime::WorkerRuntime;
use crate::task_processor::TaskProcessor;
use crate::flight_server::FlightServer;
use crate::flight_handler::FlightHandler;
use crate::retry_handler::RetryHandler;
use crate::retry_handler::RetryConfig;
use crate::metrics::MetricsCollector;
use octopus_common::{Result, OctopusError};

/// Worker service handle for task execution.
pub struct WorkerService {
    worker_id: String,
    runtime: Arc<WorkerRuntime>,
    processor: Arc<TaskProcessor>,
    flight_server: Arc<FlightServer>,
    coordinator_url: String,
    metrics: Arc<MetricsCollector>,
    retry_config: RetryConfig,
}

impl WorkerService {
    /// Create a new worker service with Flight server.
    pub fn new(coordinator_url: String, flight_port: u16) -> Result<Self> {
        let runtime = Arc::new(WorkerRuntime::new()?);

        // Create worker ID
        let worker_id = Uuid::new_v4().to_string();

        // Create metrics collector
        let metrics = Arc::new(MetricsCollector::new(worker_id.clone()));

        // Create processor with retry support
        let retry_config = RetryConfig::default();
        let processor = Arc::new(TaskProcessor::with_retry(
            runtime.cpu.clone(),
            retry_config.clone(),
            metrics.clone(),
            worker_id.clone(),
        )?);

        let handler = Arc::new(FlightHandler::new(processor.clone()));

        let flight_server = Arc::new(FlightServer::new(
            worker_id.clone(),
            flight_port,
            runtime.io.clone(),
            handler,
        ));

        tracing::info!("WorkerService created with ID: {}", worker_id);

        Ok(Self {
            worker_id,
            runtime,
            processor,
            flight_server,
            coordinator_url,
            metrics,
            retry_config,
        })
    }

    /// Get the worker ID.
    pub fn worker_id(&self) -> &str {
        &self.worker_id
    }

    /// Start the worker's task receiver loop.
    /// This runs on the IO runtime and receives tasks from the coordinator.
    pub async fn run(&self) -> Result<()> {
        tracing::info!("Worker {} starting", self.worker_id);

        // Start Arrow Flight server
        let flight_addr = self.flight_server.start().await
            .map_err(|e| OctopusError::ExecutionError(format!("Flight server error: {}", e)))?;
        tracing::info!("Arrow Flight server started on {}", flight_addr);

        // Connect to coordinator for task registration
        self.register_with_coordinator().await?;

        // Task receiver loop
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
    async fn process_task(&self, task_id: String, _plan_json: String) -> Result<String> {
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
    pub fn execute_task_sync(&self, task_id: String, _plan_json: String) -> Result<String> {
        // Use spawn_blocking on the CPU runtime for synchronous execution
        let runtime = self.runtime.cpu.as_ref();
        let worker_id = self.worker_id.clone();
        let handle = runtime.handle().spawn_blocking(move || {
            // Placeholder - actual implementation would deserialize plan and execute
            tracing::info!("Executing task {} synchronously", task_id);
            Ok(serde_json::json!({
                "task_id": task_id,
                "status": "completed",
                "worker_id": worker_id,
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