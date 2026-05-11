use std::sync::Arc;
use tokio::sync::RwLock;
use uuid::Uuid;
use tracing::info;
use datafusion::execution::context::SessionContext;
use datafusion_expr::LogicalPlan;
use crate::scheduler::QueryScheduler;

#[derive(Debug, Clone)]
pub struct Query {
    pub query_id: String,
    pub sql: String,
    pub state: QueryState,
    pub logical_plan: Option<LogicalPlan>,
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

#[derive(Debug, Clone)]
pub struct DistributedPlan {
    pub query_id: String,
    pub stages: Vec<PlanStage>,
}

#[derive(Debug, Clone)]
pub struct PlanStage {
    pub stage_id: u32,
    pub partition_count: u32,
}

pub struct QueryService {
    context: SessionContext,
    scheduler: Arc<RwLock<QueryScheduler>>,
    queries: Arc<RwLock<std::collections::HashMap<String, Query>>>,
}

impl QueryService {
    pub fn new(scheduler: Arc<RwLock<QueryScheduler>>) -> Self {
        let context = SessionContext::new();
        Self {
            context,
            scheduler,
            queries: Arc::new(RwLock::new(std::collections::HashMap::new())),
        }
    }

    pub async fn submit_query(&self, sql: &str) -> Result<String, String> {
        let query_id = Uuid::new_v4().to_string();

        // Parse SQL using DataFusion
        let df = self.context
            .sql(sql)
            .await
            .map_err(|e| format!("SQL parse error: {}", e))?;

        let logical_plan = df.logical_plan().clone();

        let query = Query {
            query_id: query_id.clone(),
            sql: sql.to_string(),
            state: QueryState::Received,
            logical_plan: Some(logical_plan),
        };

        info!("Query submitted: {} - {}", query_id, sql);
        self.queries.write().await.insert(query_id.clone(), query);
        Ok(query_id)
    }

    pub async fn plan_query(&self, query_id: &str) -> Result<DistributedPlan, String> {
        let mut queries = self.queries.write().await;
        if let Some(query) = queries.get_mut(query_id) {
            query.state = QueryState::Planning;
            info!("Planning query: {}", query_id);

            let logical_plan = query.logical_plan.take()
                .ok_or_else(|| "No logical plan found".to_string())?;

            // Create distributed plan with stage DAG
            let distributed_plan = self.create_distributed_plan(logical_plan, query_id).await?;

            query.state = QueryState::Planned;
            Ok(distributed_plan)
        } else {
            Err(format!("Query {} not found", query_id))
        }
    }

    async fn create_distributed_plan(&self, logical_plan: LogicalPlan, query_id: &str) -> Result<DistributedPlan, String> {
        // Simple stage DAG: single stage for now
        // In a full implementation, we would analyze the logical plan for exchange boundaries
        // and split into multiple stages based on data flow
        let stages = vec![PlanStage {
            stage_id: 0,
            partition_count: 1,
        }];

        info!("Created distributed plan for query {} with {} stages",
              query_id, stages.len());

        Ok(DistributedPlan {
            query_id: query_id.to_string(),
            stages,
        })
    }

    pub async fn get_query_state(&self, query_id: &str) -> Option<QueryState> {
        self.queries.read().await.get(query_id).map(|q| q.state.clone())
    }

    /// Parse SQL and return a formatted query plan for EXPLAIN command
    pub async fn explain_query(&self, sql: &str) -> Result<String, String> {
        // Parse SQL using DataFusion
        let df = self.context
            .sql(sql)
            .await
            .map_err(|e| format!("SQL parse error: {}", e))?;

        let logical_plan = df.logical_plan();

        // Format the logical plan using display
        let plan_str = format!("{}", logical_plan.display());

        // Create a structured explanation
        let explanation = format!(
            "Distributed Query Plan\n\
             ======================\n\
             \n\
             SQL: {}\n\
             \n\
             Logical Plan:\n\
             {}\n\
             \n\
             Stage DAG:\n\
             - Stage 0: Single-stage execution (partition_count=1)\n\
             \n\
             Note: Multi-stage distributed execution will be implemented\n\
             when exchange boundaries are analyzed in future iterations.",
            sql,
            plan_str
        );

        info!("Generated EXPLAIN plan for query: {}", sql);
        Ok(explanation)
    }
}