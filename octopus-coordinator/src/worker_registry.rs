use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use uuid::Uuid;
use tracing::info;

#[derive(Debug, Clone)]
pub struct PartitionInfo {
    pub partition_id: String,
    pub table_name: String,
    pub file_path: String,
}

#[derive(Debug, Clone)]
pub struct WorkerInfo {
    pub worker_id: String,
    pub host: String,
    pub port: u16,
    pub slots: u32,
    pub registered_at: std::time::Instant,
    pub last_heartbeat: std::time::Instant,
    pub partitions: Vec<PartitionInfo>,
}

pub struct WorkerRegistry {
    workers: Arc<RwLock<HashMap<String, WorkerInfo>>>,
}

impl Default for WorkerRegistry {
    fn default() -> Self {
        Self::new()
    }
}

impl WorkerRegistry {
    pub fn new() -> Self {
        Self {
            workers: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub async fn register(&self, host: String, port: u16, slots: u32) -> String {
        let worker_id = Uuid::new_v4().to_string();
        let info = WorkerInfo {
            worker_id: worker_id.clone(),
            host: host.clone(),
            port,
            slots,
            registered_at: std::time::Instant::now(),
            last_heartbeat: std::time::Instant::now(),
            partitions: Vec::new(),
        };
        self.workers.write().await.insert(worker_id.clone(), info);
        info!("Worker registered: {} at {}:{}", worker_id, host, port);
        worker_id
    }

    pub async fn unregister(&self, worker_id: &str) -> bool {
        self.workers.write().await.remove(worker_id).is_some()
    }

    pub async fn get_worker(&self, worker_id: &str) -> Option<WorkerInfo> {
        self.workers.read().await.get(worker_id).cloned()
    }

    pub async fn list_workers(&self) -> Vec<WorkerInfo> {
        self.workers.read().await.values().cloned().collect()
    }

    pub async fn update_heartbeat(&self, worker_id: &str) -> bool {
        if let Some(info) = self.workers.write().await.get_mut(worker_id) {
            info.last_heartbeat = std::time::Instant::now();
            true
        } else {
            false
        }
    }

    pub async fn register_partition(&self, worker_id: &str, partition: PartitionInfo) -> bool {
        if let Some(info) = self.workers.write().await.get_mut(worker_id) {
            info.partitions.push(partition);
            true
        } else {
            false
        }
    }
}