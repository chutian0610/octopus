use std::{collections::btree_map::OccupiedEntry, sync::Arc, time::Duration, vec};

use crate::{NodeServiceMetadata, ServiceMetadata};
use anyhow::{Ok, Result};
use dashmap::DashMap;
use octopus_rpc::{common::Entry, discovery::remote_store_service_server::RemoteStoreService};
use papaya::Operation;
use tonic::{async_trait, Request};

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
    async fn save(&self, metadata: &NodeServiceMetadata) -> Result<()> {
        // save to local store
        self.local.save(Entry::from(metadata)).await;
        Ok(())
    }

    async fn remove(&self, metadata: &NodeServiceMetadata) -> Result<()> {
        let key = metadata.node_id.as_bytes().to_vec();
        let value: Vec<u8> = vec![];
        let version = metadata.timestamp;
        let entry: Entry = Entry {
            key,
            value,
            version,
        };
        // use empty value to mark the entry as tombstone
        self.local.save(entry).await;
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
            .map(|entry| NodeServiceMetadata::from(entry))
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
    async fn get(&self, key: &str) -> Option<Entry>;
    async fn remove(&self, data: Entry);
    async fn save(&self, data: Entry);
    async fn get_all(&self) -> Vec<Entry>;
}
struct InMemoryStore {
    map: papaya::HashMap<Vec<u8>, Entry>,
}

impl InMemoryStore {
    pub fn new() -> Self {
        Self {
            map: papaya::HashMap::<Vec<u8>, Entry>::new(),
        }
    }
}

#[async_trait]
impl LocalDiscoveryStore for InMemoryStore {
    async fn get(&self, key: &str) -> Option<Entry> {
        todo!()
    }
    async fn remove(&self, data: Entry) {
        todo!()
    }
    async fn save(&self, data: Entry) {
        let map = self.map.pin();
        let mut flag = false;
        while !flag {
            let old = map.get_or_insert_with(data.key.clone(), || data.clone());
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
                let compute = map.compute(data.key.clone(), compute);
                match compute {
                    papaya::Compute::Inserted(_, _) => flag = false,
                    papaya::Compute::Updated { old: _, new: _ } => flag = true, // overwrite the flag if value is updated
                    papaya::Compute::Removed(_, _) => flag = false,
                    papaya::Compute::Aborted(_) => flag = false,
                }
            }
        }
    }
    async fn get_all(&self) -> Vec<Entry> {
        todo!()
    }
}
fn resolve(a: &Entry, b: &Entry) -> Entry {
    if a.version > b.version {
        a.clone()
    } else if a.version < b.version {
        b.clone()
    } else {
        a.clone()
    }
}
