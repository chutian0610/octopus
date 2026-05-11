//! Octopus Worker - executes distributed query tasks
//!
//! Worker process that receives tasks from coordinator, executes them
//! using DataFusion on a dedicated CPU thread pool, and exchanges data
//! via Arrow Flight.

use clap::Parser;
use tracing_subscriber::EnvFilter;
use octopus_worker::{WorkerService, WorkerRuntime};

#[derive(Parser, Debug)]
#[command(name = "octopus-worker")]
#[command(about = "Octopus Worker - Distributed task executor", long_about = None)]
struct Args {
    /// Coordinator URL for worker registration
    #[arg(long, default_value = "http://localhost:50051")]
    coordinator: String,

    /// Worker port for Arrow Flight server
    #[arg(long, default_value_t = 50052)]
    port: u16,

    /// Enable verbose logging
    #[arg(short, long)]
    verbose: bool,
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Initialize logging
    let filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new("info"));
    tracing_subscriber::fmt()
        .with_env_filter(filter)
        .init();

    let args = Args::parse();

    tracing::info!("Starting Octopus Worker");
    tracing::info!("Coordinator: {}", args.coordinator);
    tracing::info!("Flight port: {}", args.port);

    // Create worker runtime with CPU and IO separation
    let runtime = WorkerRuntime::new()?;
    tracing::info!("Worker runtime initialized");

    // Create and run worker service
    let service = WorkerService::new(args.coordinator.clone())?;

    tracing::info!("Worker {} starting", service.worker_id());
    tracing::info!("Press Ctrl+C to shutdown");

    // Run the worker service
    // Note: In this initial version, we just initialize the runtime
    // The actual task receiving via gRPC and Arrow Flight server will be in subsequent plans

    // For now, just keep the worker running
    loop {
        tokio::time::sleep(tokio::time::Duration::from_secs(60)).await;
    }
}