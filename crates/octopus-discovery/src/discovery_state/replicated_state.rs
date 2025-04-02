use std::{sync::Arc, time::Duration};

use anyhow::{Ok, Result};
use octopus_rpc::common::{Entry, NodeEntry, NodeMetadata, ServiceMetadata};
use papaya::Operation;
use tonic::async_trait;

use super::DiscoveryState;

/// ┌──────────────┐                
/// │  Replicator  │                
/// │  (Shceduled) ◄─────fetch─┐    
/// └──────┬───────┘           │    
///        Sync                │    
///        │                   │    
///    ┌───▼──────┐     ┌──────┼───┐
///    │   Local  │     │  Remote  │
///    └───▲──────┘     └──────▲───┘
///        │                   │    
///        │  ┌─────────────┐  │    
///        └──┼  Replicated │──┘    
///           │    Store    │       
///           └──────▲──────┘       
///                  │              
///           ┌──────┼──────┐       
///           │  Discovery  │       
///           │    State    │       
///           └──────▲──────┘       
///                  │              
///           ┌──────┼──────┐       
///           │  Discovery  │       
///           │   Server    │       
///           └─────────────┘       
struct ReplicatedState {
    /// The local store.
    local: Arc<dyn LocalDiscoveryStore>,
    /// The remote store.

    /// replicator.
    replicator: Replicator,
    /// the replicated state config.
    replicated_state_config: ReplicatedStateConfig,
}

impl ReplicatedState {
    /// Create a new replicated state.
    pub fn new(
        local: Arc<dyn LocalDiscoveryStore>,
        replicator: Replicator,
        replicated_state_config: ReplicatedStateConfig,
    ) -> Self {
        Self {
            local,
            replicator,
            replicated_state_config,
        }
    }
}

#[async_trait]
impl DiscoveryState for ReplicatedState {
    async fn save(&self, metadata: &NodeMetadata) -> Result<()> {
        // save to local store
        self.local
            .save(NodeEntry::from_register_node_metadata(metadata))
            .await;
        Ok(())
    }

    async fn remove(&self, metadata: &NodeMetadata) -> Result<()> {
        // use empty value to mark the entry as tombstone
        self.local
            .save(NodeEntry::from_unregister_node_metadata(metadata))
            .await;
        Ok(())
    }

    async fn list(
        &self,
        service_id: Option<&str>,
        cluster_id: Option<&str>,
    ) -> Result<Vec<ServiceMetadata>> {
        let metas = self
            .local
            .get_all()
            .await
            .into_iter()
            .filter(|entry| entry.meta.is_some())
            .map(|entry| entry.meta.unwrap())
            .flat_map(|metadata| metadata.services)
            .filter(|metadata| {
                if service_id.is_some() && metadata.service_id != service_id.unwrap() {
                    return false;
                }
                return true;
            })
            .filter(|metadata| {
                if cluster_id.is_some() && metadata.cluster_id != cluster_id.unwrap() {
                    return false;
                }
                return true;
            })
            .collect();
        Ok(metas)
    }
}

#[derive(Debug)]
struct ReplicatedStateConfig {
    expire_interval: Duration,
    tombstone_interval: Duration,
}
struct Replicator {}
/// Local store interface.
/// Local store is used to store service metadata locally.
#[async_trait]
trait LocalDiscoveryStore: Send + Sync {
    async fn get(&self, key: &str) -> Option<NodeEntry>;
    async fn remove(&self, data: NodeEntry);
    async fn save(&self, data: NodeEntry);
    async fn get_all(&self) -> Vec<NodeEntry>;
}
struct InMemoryStore {
    map: papaya::HashMap<String, NodeEntry>,
}

impl InMemoryStore {
    pub fn new() -> Self {
        Self {
            map: papaya::HashMap::<String, NodeEntry>::new(),
        }
    }
}

#[async_trait]
impl LocalDiscoveryStore for InMemoryStore {
    async fn get(&self, key: &str) -> Option<NodeEntry> {
        let map = self.map.pin();
        let result = map.get(key);
        match result {
            Some(entry) => Some(entry.clone()),
            None => None,
        }
    }
    async fn remove(&self, data: NodeEntry) {
        let map = self.map.pin();
        let _result = map.remove(&data.node_id);
    }
    async fn save(&self, data: NodeEntry) {
        let map = self.map.pin();
        let mut flag = false;
        while !flag {
            let old = map.get_or_insert_with(data.node_id.clone(), || data.clone());
            flag = true;
            if old != &data {
                let new = resolve(old, &data);
                let compute = |entry| match entry {
                    // overwrite the value if it is same as old.
                    Some((_key, value)) if value == old => Operation::Insert(new.clone()),
                    // Do nothing if it is differnet from old.
                    Some((_key, _value)) => Operation::Abort(()),
                    // Do nothing if the key does not exist
                    None => Operation::Abort(()),
                };
                let compute = map.compute(data.node_id.clone(), compute);
                match compute {
                    papaya::Compute::Inserted(_, _) => flag = false,
                    papaya::Compute::Updated { old: _, new: _ } => flag = true, // overwrite the flag if value is updated
                    papaya::Compute::Removed(_, _) => flag = false,
                    papaya::Compute::Aborted(_) => flag = false,
                }
            }
        }
    }
    async fn get_all(&self) -> Vec<NodeEntry> {
        let map = self.map.pin();
        let mut result = vec![];
        for (_, entry) in map.iter() {
            result.push(entry.clone());
        }
        result
    }
}
fn resolve(a: &NodeEntry, b: &NodeEntry) -> NodeEntry {
    if a.timestamp > b.timestamp {
        a.clone()
    } else if a.timestamp < b.timestamp {
        b.clone()
    } else {
        a.clone()
    }
}
