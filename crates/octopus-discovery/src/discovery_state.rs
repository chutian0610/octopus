use crate::ServiceMetadata;
use async_trait::async_trait;

mod in_memory_store;
pub mod replicated_state;

/// Discovery state interface.
/// is used to manage service metadata
trait DiscoveryState {
    async fn save(&self, data: ServiceMetadata);
    async fn remove(&self, data: ServiceMetadata);
    async fn list(
        &self,
        service_id: Option<&str>,
        cluster_id: Option<&str>,
    ) -> Vec<ServiceMetadata>;
}

/// Local store interface.
/// Local store is used to store service metadata locally.
#[async_trait]
trait LocalDiscoveryStore {
    async fn get(
        &self,
        service_id: &str,
        cluster_id: &str,
        instance_id: &str,
    ) -> Option<ServiceMetadata>;
    async fn remove(&self, data: ServiceMetadata);
    async fn save(&self, data: ServiceMetadata);
    async fn list(
        &self,
        service_id: Option<&str>,
        cluster_id: Option<&str>,
    ) -> Vec<ServiceMetadata>;
}
/// Remote store interface.
/// Remote store is used to for sync service metadata with remote store.
#[async_trait]
trait RemoteDiscoveryStore {
    async fn save(&self, data: ServiceMetadata);
}
