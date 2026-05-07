use clap::Parser;
use std::sync::Arc;
use octopus_executor::{ExecutorSession, QueryExecutor};
use tracing::info;
use tracing_subscriber::EnvFilter;

#[derive(Parser, Debug)]
#[command(name = "octopus")]
#[command(author = "Octopus Team")]
#[command(version = "0.1.0")]
#[command(about = "Distributed MPP query engine", long_about = None)]
struct Cli {
    #[arg(short, long)]
    sql: Option<String>,

    #[arg(short, long, default_value_t = false)]
    verbose: bool,
}

fn main() -> anyhow::Result<()> {
    let filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new("info"));

    tracing_subscriber::fmt()
        .with_env_filter(filter)
        .init();

    let cli = Cli::parse();

    let session = ExecutorSession::new()
        .map_err(|e| anyhow::anyhow!("Failed to create session: {}", e))?;

    let executor = QueryExecutor::new(Arc::new(session));

    if let Some(sql) = cli.sql {
        info!("Executing query: {}", sql);

        let rt = tokio::runtime::Runtime::new()?;

        let result = rt.block_on(executor.execute_sql_json(&sql));

        match result {
            Ok(json) => println!("{}", json),
            Err(e) => {
                eprintln!("Query error: {}", e);
                std::process::exit(1);
            }
        }
    } else {
        println!("Octopus CLI");
        println!("Use --sql to execute a query");
    }

    Ok(())
}
