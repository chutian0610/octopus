//! Octopus Worker - executes distributed query tasks
//!
//! Worker process that receives tasks from coordinator, executes them
//! using DataFusion on a dedicated CPU thread pool, and exchanges data
//! via Arrow Flight.

use clap::Parser;
use tracing_subscriber::EnvFilter;
use octopus_worker::WorkerService;

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

    // Create worker service with Flight server
    let service = WorkerService::new(args.coordinator.clone(), args.port)?;

    tracing::info!("Worker {} starting", service.worker_id());
    tracing::info!("Press Ctrl+C to shutdown");

    // Run the worker service with Flight server
    service.run().await?;

    Ok(())
}