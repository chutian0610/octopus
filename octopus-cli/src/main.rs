use clap::Parser;
use std::io::Read;
use std::sync::Arc;
use octopus_executor::{
    ExecutorSession,
    QueryExecutor,
    DataSourceRegistrar,
    logging::{self, LogFormat, QueryTrace},
};
use tokio::runtime::Runtime;

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

    /// Coordinator address for distributed mode (REPL/batch)
    #[arg(long, default_value = "http://localhost:50051")]
    coordinator: String,

    /// SQL file for batch mode (reads from stdin if not specified)
    #[arg(long)]
    file: Option<String>,
}

/// HTTP-based coordinator client for distributed query execution
#[derive(Clone)]
struct CoordinatorClient {
    base_url: String,
    rt: Arc<Runtime>,
}

impl CoordinatorClient {
    fn new(base_url: &str) -> anyhow::Result<Self> {
        let rt = Runtime::new()?;
        Ok(Self {
            base_url: base_url.to_string(),
            rt: Arc::new(rt),
        })
    }

    fn submit_query(&self, sql: &str) -> anyhow::Result<String> {
        let url = format!("{}/query/submit", self.base_url);
        let client = reqwest::blocking::Client::new();
        let resp = client
            .post(&url)
            .json(&serde_json::json!({ "sql": sql }))
            .timeout(std::time::Duration::from_secs(30))
            .send()?;

        if resp.status().is_success() {
            let result: serde_json::Value = resp.json()?;
            Ok(result["query_id"].as_str().unwrap_or("").to_string())
        } else {
            Err(anyhow::anyhow!("Query submission failed: {}", resp.status()))
        }
    }

    fn get_query_state(&self, query_id: &str) -> anyhow::Result<String> {
        let url = format!("{}/query/{}", self.base_url, query_id);
        let client = reqwest::blocking::Client::new();
        let resp = client
            .get(&url)
            .timeout(std::time::Duration::from_secs(10))
            .send()?;

        if resp.status().is_success() {
            let result: serde_json::Value = resp.json()?;
            Ok(result["state"].as_str().unwrap_or("UNKNOWN").to_string())
        } else {
            Err(anyhow::anyhow!("Query state lookup failed: {}", resp.status()))
        }
    }

    fn explain_query(&self, sql: &str) -> anyhow::Result<String> {
        let url = format!("{}/query/explain", self.base_url);
        let client = reqwest::blocking::Client::new();
        let resp = client
            .post(&url)
            .json(&serde_json::json!({ "sql": sql }))
            .timeout(std::time::Duration::from_secs(30))
            .send()?;

        if resp.status().is_success() {
            let result: serde_json::Value = resp.json()?;
            Ok(serde_json::to_string_pretty(&result).unwrap_or_default())
        } else {
            Err(anyhow::anyhow!("EXPLAIN failed: {}", resp.status()))
        }
    }

    fn poll_for_completion(&self, query_id: &str, max_attempts: u32) -> anyhow::Result<String> {
        for _ in 0..max_attempts {
            let state = self.get_query_state(query_id)?;
            match state.as_str() {
                "Completed" => return Ok(state),
                "Failed" => return Err(anyhow::anyhow!("Query failed")),
                _ => {
                    std::thread::sleep(std::time::Duration::from_millis(100));
                }
            }
        }
        Ok("Still running".to_string())
    }
}

fn main() -> anyhow::Result<()> {
    let cli = Cli::parse();

    match cli.mode.as_str() {
        "local" => run_local(cli)?,
        "interactive" => run_repl(cli)?,
        "batch" => run_batch(cli)?,
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

fn run_repl(cli: Cli) -> anyhow::Result<()> {
    let client = CoordinatorClient::new(&cli.coordinator)?;

    println!("Octopus Interactive REPL");
    println!("Coordinator: {}", cli.coordinator);
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
                println!("  explain <sql> - Show distributed query plan");
                println!("  Any SQL query - Execute the query");
            },
            _ => {
                // Check for EXPLAIN prefix
                if input.to_lowercase().starts_with("explain ") {
                    let sql = &input[8..].trim();
                    match client.explain_query(sql) {
                        Ok(plan) => {
                            println!("Distributed Query Plan:");
                            println!("{}", plan);
                        }
                        Err(e) => {
                            eprintln!("EXPLAIN error: {}", e);
                        }
                    }
                } else {
                    // Regular query execution
                    match client.submit_query(input) {
                        Ok(query_id) => {
                            println!("Query submitted: {}", query_id);
                            match client.poll_for_completion(&query_id, 100) {
                                Ok(state) => println!("Query state: {}", state),
                                Err(e) => eprintln!("Poll error: {}", e),
                            }
                        }
                        Err(e) => {
                            eprintln!("Query error: {}", e);
                        }
                    }
                }
            }
        }
    }

    println!("Goodbye!");
    Ok(())
}

fn run_batch(cli: Cli) -> anyhow::Result<()> {
    let client = CoordinatorClient::new(&cli.coordinator)?;

    // Read SQL from file or stdin
    let sql_content = if let Some(file_path) = &cli.file {
        std::fs::read_to_string(file_path)
            .map_err(|e| anyhow::anyhow!("Failed to read file {}: {}", file_path, e))?
    } else {
        // Read from stdin
        let mut content = String::new();
        std::io::stdin().read_to_string(&mut content)?;
        content
    };

    // Parse and execute statements (separated by semicolons)
    let mut current_stmt = String::new();
    for line in sql_content.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with("--") {
            continue; // Skip empty lines and comments
        }

        current_stmt.push_str(line);
        current_stmt.push(' ');

        if trimmed.ends_with(';') {
            let stmt = current_stmt.trim_end_matches(';').trim();
            if !stmt.is_empty() {
                if stmt.to_lowercase().starts_with("explain ") {
                    let sql = &stmt[8..].trim();
                    match client.explain_query(sql) {
                        Ok(plan) => println!("{}", plan),
                        Err(e) => eprintln!("EXPLAIN error: {}", e),
                    }
                } else {
                    match client.submit_query(stmt) {
                        Ok(query_id) => {
                            println!("Query {}: {}", query_id, stmt);
                            match client.poll_for_completion(&query_id, 100) {
                                Ok(state) => println!("  -> {}", state),
                                Err(e) => eprintln!("  -> Poll error: {}", e),
                            }
                        }
                        Err(e) => {
                            eprintln!("Query error: {}", e);
                        }
                    }
                }
            }
            current_stmt.clear();
        }
    }

    Ok(())
}