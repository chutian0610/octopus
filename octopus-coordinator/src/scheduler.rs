use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use uuid::Uuid;
use tracing::info;
use super::worker_registry::{WorkerRegistry, WorkerInfo};

#[derive(Debug, Clone)]
pub struct Task {
    pub task_id: String,
    pub query_id: String,
    pub stage: u32,
    pub partition: u32,
    pub assigned_worker: Option<String>,
    pub required_partitions: Vec<String>,
}

pub struct QueryScheduler {
    registry: Arc<WorkerRegistry>,
    pending_tasks: Arc<RwLock<HashMap<String, Task>>>,
    task_counter: Arc<RwLock<u32>>,
}

impl QueryScheduler {
    pub fn new(registry: Arc<WorkerRegistry>) -> Self {
        Self {
            registry,
            pending_tasks: Arc::new(RwLock::new(HashMap::new())),
            task_counter: Arc::new(RwLock::new(0)),
        }
    }

    pub async fn create_task(&self, query_id: &str, stage: u32, partition: u32, required_partitions: Vec<String>) -> Task {
        let task_id = Uuid::new_v4().to_string();
        let task = Task {
            task_id: task_id.clone(),
            query_id: query_id.to_string(),
            stage,
            partition,
            assigned_worker: None,
            required_partitions,
        };
        self.pending_tasks.write().await.insert(task_id, task.clone());
        info!("Created task {} for query {} stage {} partition {}",
              task.task_id, query_id, stage, partition);
        task
    }

    pub async fn assign_task(&self, task_id: &str) -> Option<String> {
        let mut tasks = self.pending_tasks.write().await;
        let task = tasks.get_mut(task_id)?;

        let workers = self.registry.list_workers().await;
        if workers.is_empty() {
            return None;
        }

        // If task has partition requirements, score workers by locality
        let best_worker = if task.required_partitions.is_empty() {
            // Fall back to round-robin when no locality info
            let idx = *self.task_counter.read().await as usize % workers.len();
            workers.get(idx).cloned()
        } else {
            // Find worker with most locality (most matching partitions)
            self.find_best_worker(&workers, &task.required_partitions).await
        };

        if let Some(worker) = best_worker {
            task.assigned_worker = Some(worker.worker_id.clone());

            if task.required_partitions.is_empty() {
                let mut counter = self.task_counter.write().await;
                *counter += 1;
            }

            info!("Assigned task {} to worker {} (locality-aware)",
                  task_id, worker.worker_id);
            Some(worker.worker_id.clone())
        } else {
            None
        }
    }

    async fn find_best_worker(
        &self,
        workers: &[WorkerInfo],
        required_partitions: &[String],
    ) -> Option<WorkerInfo> {
        let mut best_worker: Option<WorkerInfo> = None;
        let mut best_score: usize = 0;

        for worker in workers {
            let score = worker.partitions.iter()
                .filter(|p| required_partitions.contains(&p.partition_id))
                .count();

            if score > best_score {
                best_score = score;
                best_worker = Some(worker.clone());
            }
        }

        best_worker
    }

    pub async fn complete_task(&self, task_id: &str) {
        self.pending_tasks.write().await.remove(task_id);
    }

    pub async fn get_task(&self, task_id: &str) -> Option<Task> {
        self.pending_tasks.read().await.get(task_id).cloned()
    }
}