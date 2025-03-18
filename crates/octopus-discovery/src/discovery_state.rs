use crate::ServiceMetadata;

pub mod replicated_state;

/// Discovery state interface.
/// is used to manage service metadata
trait DiscoveryState {
    async fn save(&self, services: Vec<ServiceMetadata>);
    async fn remove(&self, services: Vec<ServiceMetadata>);
    async fn list(
        &self,
        service_id: Option<&str>,
        cluster_id: Option<&str>,
    ) -> Vec<ServiceMetadata>;
}
