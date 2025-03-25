use std::time::Duration;

use octopus_rpc::discovery::remote_store_service_server::RemoteStoreService;
use serde::de::value;
use tonic::async_trait;

use crate::{NodeServiceMetadata, ServiceMetadata};

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
struct ReplicatedState<L: LocalDiscoveryStore, R: RemoteStoreService> {
    /// The local store.
    local: L,
    /// The remote store.
    remote: R,
    /// replicator.
    replicator: Replicator,
    /// the replicated state config.
    replicated_state_config: ReplicatedStateConfig,
}

impl<L, R> ReplicatedState<L, R>
where
    L: LocalDiscoveryStore,
    R: RemoteStoreService,
{
    /// Create a new replicated state.
    pub fn new(
        local: L,
        remote: R,
        replicator: Replicator,
        replicated_state_config: ReplicatedStateConfig,
    ) -> Self {
        Self {
            local,
            remote,
            replicator,
            replicated_state_config,
        }
    }
}

impl<L, R> DiscoveryState for ReplicatedState<L, R>
where
    L: LocalDiscoveryStore,
    R: RemoteStoreService,
{
    async fn save(&self, metadata: NodeServiceMetadata) {
        todo!()
    }

    async fn remove(&self, metadata: NodeServiceMetadata) {
        todo!()
    }

    async fn list(
        &self,
        service_id: Option<&str>,
        cluster_id: Option<&str>,
    ) -> Vec<crate::ServiceMetadata> {
        todo!()
    }
}

#[derive(Debug)]
struct ReplicatedStateConfig {
    expire_interval: Duration,
    tombstone_interval: Duration,
}

#[derive(Debug)]
struct Entry {
    key: Vec<u8>,
    value: Vec<u8>,
    version: i64,
}
impl Entry {
    fn new(key: Vec<u8>, value: Vec<u8>, version: i64) -> Self {
        Self {
            key,
            value,
            version,
        }
    }
    fn from_node_service_metadata(metadata: &NodeServiceMetadata) -> Self {
        let key = metadata.node_id.as_bytes().to_vec();
        let value = serde_json::to_vec(metadata).unwrap();
        let version = metadata.timestamp;
        Self::new(key, value, version)
    }
}

/// Local store interface.
/// Local store is used to store service metadata locally.
#[async_trait]
trait LocalDiscoveryStore {
    async fn get(&self, key: &str) -> Option<Entry>;
    async fn remove(&self, data: Entry);
    async fn save(&self, data: Entry);
    async fn get_all(&self) -> Vec<Entry>;
}
/// Remote store interface.
/// Remote store is used to for sync service metadata with remote store.
#[async_trait]
trait RemoteDiscoveryStore {
    async fn save(&self, datas: Vec<Entry>);
}

struct Replicator {}
struct InMemoryStore {}

#[async_trait]
impl LocalDiscoveryStore for InMemoryStore {
    async fn get(&self, key: &str) -> Option<Entry> {
        todo!()
    }
    async fn remove(&self, data: Entry) {
        todo!()
    }
    async fn save(&self, data: Entry) {
        todo!()
    }
    async fn get_all(&self) -> Vec<Entry> {
        todo!()
    }
}
