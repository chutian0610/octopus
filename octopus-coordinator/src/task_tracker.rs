//! Task tracker for coordinator-side task monitoring and rescheduling.
//!
//! Tracks task state, coordinates retries across workers, and handles
//! rescheduling when workers fail persistently (per D-04).

use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{info, warn};
use uuid::Uuid;

/// Task state tracked by coordinator.
#[derive(Debug, Clone, PartialEq)]
pub enum TaskState {
    Pending,
    Assigned { worker_id: String },
    Running { worker_id: String, attempt: u32 },
    Completed,
    Failed { error: String },
    Rescheduling { reason: String },
}

/// Task tracked by the coordinator.
#[derive(Debug, Clone)]
pub struct TrackedTask {
    pub task_id: String,
    pub query_id: String,
    pub stage: u32,
    pub partition: u32,
    pub state: TaskState,
    pub assigned_worker: Option<String>,
    pub retry_count: u32,
    pub created_at: std::time::Instant,
    pub last_update: std::time::Instant,
}

impl TrackedTask {
    pub fn new(task_id: String, query_id: String, stage: u32, partition: u32) -> Self {
        let now = std::time::Instant::now();
        Self {
            task_id,
            query_id,
            stage,
            partition,
            state: TaskState::Pending,
            assigned_worker: None,
            retry_count: 0,
            created_at: now,
            last_update: now,
        }
    }
}

/// Task tracker for coordinator-side coordination.
pub struct TaskTracker {
    tasks: Arc<RwLock<HashMap<String, TrackedTask>>>,
    max_retry_per_worker: u32,
}

impl TaskTracker {
    pub fn new(max_retry_per_worker: u32) -> Self {
        Self {
            tasks: Arc::new(RwLock::new(HashMap::new())),
            max_retry_per_worker,
        }
    }

    /// Create and track a new task.
    pub async fn create_task(
        &self,
        query_id: String,
        stage: u32,
        partition: u32,
    ) -> String {
        let task_id = Uuid::new_v4().to_string();
        let task = TrackedTask::new(task_id.clone(), query_id.clone(), stage, partition);

        let mut tasks = self.tasks.write().await;
        tasks.insert(task_id.clone(), task);

        info!("Created task {} for query {} stage {} partition {}",
              task_id, query_id, stage, partition);

        task_id
    }

    /// Assign a task to a worker.
    pub async fn assign_task(&self, task_id: &str, worker_id: &str) -> Option<TrackedTask> {
        let mut tasks = self.tasks.write().await;

        if let Some(task) = tasks.get_mut(task_id) {
            task.assigned_worker = Some(worker_id.to_string());
            task.state = TaskState::Assigned { worker_id: worker_id.to_string() };
            task.last_update = std::time::Instant::now();
            info!("Assigned task {} to worker {}", task_id, worker_id);
            return Some(task.clone());
        }

        None
    }

    /// Mark a task as running.
    pub async fn start_task(&self, task_id: &str, worker_id: &str, attempt: u32) {
        let mut tasks = self.tasks.write().await;

        if let Some(task) = tasks.get_mut(task_id) {
            task.state = TaskState::Running { worker_id: worker_id.to_string(), attempt };
            task.last_update = std::time::Instant::now();
        }
    }

    /// Mark a task as completed.
    pub async fn complete_task(&self, task_id: &str) {
        let mut tasks = self.tasks.write().await;

        if let Some(task) = tasks.get_mut(task_id) {
            task.state = TaskState::Completed;
            task.last_update = std::time::Instant::now();
            info!("Task {} completed", task_id);
        }
    }

    /// Mark a task as failed and determine if rescheduling is needed.
    pub async fn fail_task(
        &self,
        task_id: &str,
        error: String,
    ) -> RescheduleDecision {
        let mut tasks = self.tasks.write().await;

        if let Some(task) = tasks.get_mut(task_id) {
            let worker_id = task.assigned_worker.clone().unwrap_or_default();
            task.retry_count += 1;

            if task.retry_count <= self.max_retry_per_worker {
                warn!("Task {} failed (attempt {}), will retry on same worker {}",
                      task_id, task.retry_count, worker_id);
                task.state = TaskState::Pending;
                task.last_update = std::time::Instant::now();
                RescheduleDecision::RetrySameWorker { worker_id }
            } else {
                warn!("Task {} exhausted retries on worker {}, rescheduling",
                      task_id, worker_id);
                task.state = TaskState::Rescheduling {
                    reason: format!("Exceeded max retries ({})", self.max_retry_per_worker)
                };
                task.assigned_worker = None;
                task.last_update = std::time::Instant::now();
                RescheduleDecision::Reschedule { reason: error }
            }
        } else {
            RescheduleDecision::Reschedule { reason: error }
        }
    }

    /// Get task state.
    pub async fn get_task_state(&self, task_id: &str) -> Option<TaskState> {
        let tasks = self.tasks.read().await;
        tasks.get(task_id).map(|t| t.state.clone())
    }

    /// List tasks by state.
    pub async fn list_tasks_by_state(&self, state: &TaskState) -> Vec<TrackedTask> {
        let tasks = self.tasks.read().await;
        tasks.values()
            .filter(|t| &t.state == state)
            .cloned()
            .collect()
    }

    /// List tasks for a query.
    pub async fn list_query_tasks(&self, query_id: &str) -> Vec<TrackedTask> {
        let tasks = self.tasks.read().await;
        tasks.values()
            .filter(|t| t.query_id == query_id)
            .cloned()
            .collect()
    }
}

/// Decision for task rescheduling.
#[derive(Debug)]
pub enum RescheduleDecision {
    RetrySameWorker { worker_id: String },
    Reschedule { reason: String },
}

impl Default for TaskTracker {
    fn default() -> Self {
        Self::new(3)  // Default: 3 retries per worker
    }
}