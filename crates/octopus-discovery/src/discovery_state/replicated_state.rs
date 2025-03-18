use crate::ServiceMetadata;

use super::{DiscoveryState, LocalDiscoveryStore, RemoteDiscoveryStore};

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

struct ReplicatedState<L: LocalDiscoveryStore, R: RemoteDiscoveryStore> {
    /// The local store.
    local: L,
    /// The remote store.
    remote: R,
    /// replicator.
    replicator: Replicator,
}
impl<L, R> DiscoveryState for ReplicatedState<L, R>
where
    L: LocalDiscoveryStore,
    R: RemoteDiscoveryStore,
{
    async fn save(&self, datas: Vec<ServiceMetadata>) {
        self.local.save(datas).await;
    }

    async fn remove(&self, datas: Vec<ServiceMetadata>) {
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

struct Replicator {}
