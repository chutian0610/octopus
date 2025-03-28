use std::time::Duration;

use crate::NodeServiceMetadata;
use anyhow::{Ok, Result};
use octopus_rpc::{common::Entry, discovery::remote_store_service_server::RemoteStoreService};
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
    local: Box<dyn LocalDiscoveryStore>,
    /// The remote store.
    remote: Box<dyn RemoteStoreService>,
    /// replicator.
    replicator: Replicator,
    /// the replicated state config.
    replicated_state_config: ReplicatedStateConfig,
}

impl ReplicatedState {
    /// Create a new replicated state.
    pub fn new(
        local: Box<dyn LocalDiscoveryStore>,
        remote: Box<dyn RemoteStoreService>,
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

impl DiscoveryState for ReplicatedState {
    async fn save(&self, metadata: NodeServiceMetadata) -> Result<()> {
        let meta = &metadata;
        // save to local store
        self.local.save(meta.to_entry()).await;
        // save to remote store
        let _response = self.remote.save(Request::new(meta.to_entry())).await;
        Ok(())
    }

    async fn remove(&self, metadata: NodeServiceMetadata) -> Result<()> {
        todo!()
    }

    async fn list(
        &self,
        service_id: Option<&str>,
        cluster_id: Option<&str>,
    ) -> Result<Vec<crate::ServiceMetadata>> {
        todo!()
    }
}

#[derive(Debug)]
struct ReplicatedStateConfig {
    expire_interval: Duration,
    tombstone_interval: Duration,
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
