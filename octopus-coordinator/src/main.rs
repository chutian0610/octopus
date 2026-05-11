use std::sync::Arc;
use tokio::net::TcpListener;
use tokio::sync::RwLock;
use tracing_subscriber::EnvFilter;
use clap::Parser;
use axum::{
    Router,
    routing::{post, get},
    extract::{Path, State},
    Json,
};
use tower_http::cors::{CorsLayer, Any};
use serde::{Deserialize, Serialize};

use octopus_coordinator::{WorkerRegistry, QueryScheduler, QueryService, CoordinatorServer};

#[derive(Parser, Debug)]
#[command(name = "octopus-coordinator")]
#[command(about = "Octopus Coordinator - Distributed query orchestration", long_about = None)]
struct Args {
    #[arg(long, default_value_t = 50051)]
    port: u16,

    #[arg(long, default_value = "0.0.0.0")]
    host: String,

    #[arg(long, default_value_t = 50051)]
    http_port: u16,
}

// Application state shared across handlers
#[derive(Clone)]
struct AppState {
    coordinator_server: Arc<CoordinatorServer>,
}

#[derive(Deserialize)]
struct SubmitQueryRequest {
    sql: String,
}

#[derive(Serialize)]
struct SubmitQueryResponse {
    query_id: String,
}

#[derive(Serialize)]
struct QueryStateResponse {
    state: String,
}

#[derive(Deserialize)]
struct ExplainQueryRequest {
    sql: String,
}

async fn submit_query_handler(
    State(state): State<AppState>,
    Json(payload): Json<SubmitQueryRequest>,
) -> Json<SubmitQueryResponse> {
    match state.coordinator_server.submit_query(&payload.sql).await {
        Ok(query_id) => Json(SubmitQueryResponse { query_id }),
        Err(e) => {
            tracing::error!("Submit query error: {}", e);
            Json(SubmitQueryResponse {
                query_id: format!("error: {}", e),
            })
        }
    }
}

async fn explain_query_handler(
    State(state): State<AppState>,
    Json(payload): Json<ExplainQueryRequest>,
) -> Json<serde_json::Value> {
    match state.coordinator_server.explain_query(&payload.sql).await {
        Ok(plan) => Json(serde_json::json!({ "plan": plan })),
        Err(e) => {
            tracing::error!("Explain query error: {}", e);
            Json(serde_json::json!({ "error": e }))
        }
    }
}

async fn query_state_handler(
    State(state): State<AppState>,
    Path(query_id): Path<String>,
) -> Json<QueryStateResponse> {
    match state.coordinator_server.get_query_state(&query_id).await {
        Some(state_str) => Json(QueryStateResponse {
            state: format!("{:?}", state_str),
        }),
        None => Json(QueryStateResponse {
            state: "NotFound".to_string(),
        }),
    }
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
    let coordinator_server = Arc::new(CoordinatorServer::new(
        registry,
        scheduler,
        query_service,
    ));

    let app_state = AppState {
        coordinator_server: coordinator_server.clone(),
    };

    // Build router with CORS
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    let app = Router::new()
        .route("/query/submit", post(submit_query_handler))
        .route("/query/explain", post(explain_query_handler))
        .route("/query/state/:query_id", get(query_state_handler))
        .route("/query/state/:query_id", post(query_state_handler))
        .layer(cors)
        .with_state(app_state);

    let addr = format!("{}:{}", args.host, args.http_port);
    println!("Octopus Coordinator");
    println!("HTTP server listening on {}", addr);
    println!("Press Ctrl+C to shutdown");

    let listener = TcpListener::bind(&addr).await?;
    axum::serve(listener, app).await?;

    Ok(())
}