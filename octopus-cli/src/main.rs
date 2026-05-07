use clap::Parser;
use std::sync::Arc;
use octopus_executor::{
    ExecutorSession,
    QueryExecutor,
    DataSourceRegistrar,
    logging::{self, LogFormat, QueryTrace},
};

#[derive(Parser, Debug)]
#[command(name = "octopus")]
#[command(about = "Distributed MPP query engine", long_about = None)]
struct Cli {
    #[arg(short, long)]
    sql: Option<String>,

    #[arg(long, default_value = "pretty")]
    log_format: String,

    #[arg(long)]
    parquet: Vec<String>,

    #[arg(long)]
    csv: Vec<String>,

    #[arg(long)]
    json: Vec<String>,

    #[arg(short, long, default_value_t = false)]
    verbose: bool,

    #[arg(long, default_value = "local")]
    mode: String,
}

fn main() -> anyhow::Result<()> {
    let cli = Cli::parse();

    match cli.mode.as_str() {
        "local" => run_local(cli)?,
        "interactive" => run_repl(cli)?,
        _ => run_local(cli)?,
    }

    Ok(())
}

fn run_local(cli: Cli) -> anyhow::Result<()> {
    let format = match cli.log_format.as_str() {
        "structured" | "json" => LogFormat::Structured,
        _ => LogFormat::Pretty,
    };
    logging::init_tracing(format);

    let session = ExecutorSession::new()
        .map_err(|e| anyhow::anyhow!("Failed to create session: {}", e))?;

    let ctx = session.context().clone();
    let registrar = DataSourceRegistrar::new(ctx.clone());
    let executor = QueryExecutor::new(Arc::new(session));

    let rt = tokio::runtime::Runtime::new()?;

    for path in &cli.parquet {
        let name = std::path::Path::new(path)
            .file_stem()
            .and_then(|s| s.to_str())
            .unwrap_or("parquet_table");

        rt.block_on(registrar.register_parquet(name, path))
            .map_err(|e| anyhow::anyhow!("Failed to register Parquet {}: {}", path, e))?;
    }

    for path in &cli.csv {
        let name = std::path::Path::new(path)
            .file_stem()
            .and_then(|s| s.to_str())
            .unwrap_or("csv_table");

        rt.block_on(registrar.register_csv(name, path, true))
            .map_err(|e| anyhow::anyhow!("Failed to register CSV {}: {}", path, e))?;
    }

    for path in &cli.json {
        let name = std::path::Path::new(path)
            .file_stem()
            .and_then(|s| s.to_str())
            .unwrap_or("json_table");

        rt.block_on(registrar.register_json(name, path))
            .map_err(|e| anyhow::anyhow!("Failed to register JSON {}: {}", path, e))?;
    }

    if let Some(sql) = cli.sql {
        let trace = QueryTrace::new(&sql);
        trace.log_start();

        match rt.block_on(executor.execute_sql_json(&sql)) {
            Ok(result) => {
                let row_count = result.matches('\n').count();
                trace.log_complete(row_count);
                println!("{}", result);
            },
            Err(e) => {
                trace.log_error(&e.to_string());
                eprintln!("Query error: {}", e);
                std::process::exit(1);
            }
        }
    } else {
        println!("Octopus CLI (local mode)");
        println!("Use --sql to execute a query");
        println!("Use --mode interactive for REPL mode");
    }

    Ok(())
}

fn run_repl(_cli: Cli) -> anyhow::Result<()> {
    println!("Octopus Interactive REPL");
    println!("Type 'exit' or 'quit' to exit");
    println!("Type 'help' for commands");
    println!();

    loop {
        print!("octopus> ");
        std::io::Write::flush(&mut std::io::stdout())?;

        let mut input = String::new();
        if std::io::stdin().read_line(&mut input)? == 0 {
            break;
        }

        let input = input.trim();
        if input.is_empty() {
            continue;
        }

        match input.to_lowercase().as_str() {
            "exit" | "quit" => break,
            "help" => {
                println!("Commands:");
                println!("  exit, quit - Exit the REPL");
                println!("  help - Show this help");
                println!("  Any SQL query - Execute the query");
            },
            _ => {
                println!("Executing: {}", input);
            }
        }
    }

    println!("Goodbye!");
    Ok(())
}