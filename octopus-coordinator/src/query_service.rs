use std::sync::Arc;
use tokio::sync::RwLock;
use uuid::Uuid;
use tracing::info;
use crate::scheduler::QueryScheduler;

#[derive(Debug, Clone)]
pub struct Query {
    pub query_id: String,
    pub sql: String,
    pub state: QueryState,
}

#[derive(Debug, Clone, PartialEq)]
pub enum QueryState {
    Received,
    Planning,
    Planned,
    Executing,
    Completed,
    Failed,
}

pub struct QueryService {
    scheduler: Arc<RwLock<QueryScheduler>>,
    queries: Arc<RwLock<std::collections::HashMap<String, Query>>>,
}

impl QueryService {
    pub fn new(scheduler: Arc<RwLock<QueryScheduler>>) -> Self {
        Self {
            scheduler,
            queries: Arc::new(RwLock::new(std::collections::HashMap::new())),
        }
    }

    pub async fn submit_query(&self, sql: &str) -> String {
        let query_id = Uuid::new_v4().to_string();
        let query = Query {
            query_id: query_id.clone(),
            sql: sql.to_string(),
            state: QueryState::Received,
        };
        info!("Query submitted: {} - {}", query_id, sql);
        self.queries.write().await.insert(query_id.clone(), query);
        query_id
    }

    pub async fn plan_query(&self, query_id: &str) -> Result<(), String> {
        let mut queries = self.queries.write().await;
        if let Some(query) = queries.get_mut(query_id) {
            query.state = QueryState::Planning;
            info!("Planning query: {}", query_id);
            query.state = QueryState::Planned;
            Ok(())
        } else {
            Err(format!("Query {} not found", query_id))
        }
    }

    pub async fn get_query_state(&self, query_id: &str) -> Option<QueryState> {
        self.queries.read().await.get(query_id).map(|q| q.state.clone())
    }
}