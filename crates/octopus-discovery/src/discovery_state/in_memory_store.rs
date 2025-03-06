use super::LocalDiscoveryStore;
use crate::ServiceMetadata;
use async_trait::async_trait;

struct InMemoryStore {}

#[async_trait]
impl LocalDiscoveryStore for InMemoryStore {
    async fn get(
        &self,
        service_id: &str,
        cluster_id: &str,
        instance_id: &str,
    ) -> Option<ServiceMetadata> {
        todo!()
    }

    async fn remove(&self, data: ServiceMetadata) {
        todo!()
    }

    async fn save(&self, data: ServiceMetadata) {
        todo!()
    }

    async fn list(
        &self,
        service_id: Option<&str>,
        cluster_id: Option<&str>,
    ) -> Vec<ServiceMetadata> {
        todo!()
    }
}
