use anyhow::Result;
use octopus_rpc::common::{NodeMetadata, ServiceMetadata};
use tonic::async_trait;
pub mod replicated_state;

/// Discovery state interface.
/// is used to manage service metadata

#[async_trait]
pub trait DiscoveryState: Send + Sync {
    async fn save(&self, metadata: &NodeMetadata) -> Result<()>;
    async fn remove(&self, metadata: &NodeMetadata) -> Result<()>;
    async fn list(
        &self,
        service_id: Option<&str>,
        cluster_id: Option<&str>,
    ) -> Result<Vec<ServiceMetadata>>;
}
