use clap::Parser;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing_subscriber::EnvFilter;
use octopus_coordinator::{WorkerRegistry, QueryScheduler, QueryService, CoordinatorServer};

#[derive(Parser, Debug)]
#[command(name = "octopus-coordinator")]
#[command(about = "Octopus Coordinator - Distributed query orchestration", long_about = None)]
struct Args {
    #[arg(long, default_value_t = 50051)]
    port: u16,

    #[arg(long, default_value = "0.0.0.0")]
    host: String,
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new("info"));
    tracing_subscriber::fmt()
        .with_env_filter(filter)
        .init();

    let args = Args::parse();

    let registry = Arc::new(WorkerRegistry::new());
    let scheduler = Arc::new(RwLock::new(QueryScheduler::new(registry.clone())));
    let query_service = Arc::new(QueryService::new(scheduler.clone()));
    let _server = CoordinatorServer::new(registry, scheduler, query_service);

    println!("Octopus Coordinator");
    println!("Listening on {}:{}", args.host, args.port);
    println!("Press Ctrl+C to shutdown");

    loop {
        tokio::time::sleep(tokio::time::Duration::from_secs(60)).await;
    }
}