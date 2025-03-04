use crate::ServiceMetadata;

/// Discovery state interface
trait DiscoveryState {
    async fn save(&self, data: ServiceMetadata);
    async fn remove(&self, data: ServiceMetadata);
    async fn list(
        &self,
        service_id: Option<&str>,
        cluster_id: Option<&str>,
    ) -> Vec<ServiceMetadata>;
}

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
trait RemoteDiscoveryStore {
    async fn save(&self, data: ServiceMetadata);
}
