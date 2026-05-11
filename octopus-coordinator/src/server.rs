use std::sync::Arc;
use tokio::sync::RwLock;
use crate::{WorkerRegistry, QueryScheduler, QueryService};
use crate::scheduler::Task;
use crate::query_service::QueryState;

pub struct CoordinatorServer {
    registry: Arc<WorkerRegistry>,
    scheduler: Arc<RwLock<QueryScheduler>>,
    query_service: Arc<QueryService>,
}

impl CoordinatorServer {
    pub fn new(
        registry: Arc<WorkerRegistry>,
        scheduler: Arc<RwLock<QueryScheduler>>,
        query_service: Arc<QueryService>,
    ) -> Self {
        Self { registry, scheduler, query_service }
    }

    pub async fn register_worker(&self, host: String, port: u16, slots: u32) -> String {
        self.registry.register(host, port, slots).await
    }

    pub async fn create_task(&self, query_id: &str, stage: u32, partition: u32) -> Task {
        let scheduler = self.scheduler.read().await;
        scheduler.create_task(query_id, stage, partition, Vec::new()).await
    }

    pub async fn submit_query(&self, sql: &str) -> Result<String, String> {
        self.query_service.submit_query(sql).await
    }

    pub async fn get_query_state(&self, query_id: &str) -> Option<QueryState> {
        self.query_service.get_query_state(query_id).await
    }
}