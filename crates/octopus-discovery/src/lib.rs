use std::time::SystemTime;

use octopus_common::time_util::to_millis;
use octopus_rpc::{
    common::{Entry, ServiceInstance},
    discovery::NodeAnnounceReq,
};
use serde::{Deserialize, Serialize};

///           +-------------+     
///           |             |     
///    +------+  Discovery  |     
///    |      |    Server   |     
///    |      |             |     
///    |      +--------^----+     
///    |               |          
///    |LookUp         |Announce  
///    |               |UnAnnounce
///    |               |          
/// +--v----------+    |          
/// |             |    |          
/// |  Discovery  +----+          
/// |    Client   |               
/// |             |               
/// +-------------+               
pub mod discovery_client;
pub mod discovery_server;

mod discovery_state;

#[derive(Debug, Serialize, Deserialize)]
pub struct NodeServiceMetadata {
    node_id: String,
    timestamp: i64,
    services: Vec<ServiceMetadata>,
}
impl NodeServiceMetadata {
    pub fn new(node_id: String, timestamp: i64, services: Vec<ServiceMetadata>) -> Self {
        Self {
            node_id,
            timestamp,
            services,
        }
    }
}
impl From<&NodeServiceMetadata> for Entry {
    fn from(item: &NodeServiceMetadata) -> Self {
        let key = item.node_id.as_bytes().to_vec();
        let value = serde_json::to_vec(item).unwrap();
        let version = item.timestamp;
        Entry {
            key,
            value,
            version,
        }
    }
}
impl From<Entry> for NodeServiceMetadata {
    fn from(item: Entry) -> Self {
        return serde_json::from_slice(&item.value).unwrap();
    }
}

impl From<&NodeAnnounceReq> for NodeServiceMetadata {
    fn from(req: &NodeAnnounceReq) -> Self {
        let timestamp = to_millis(SystemTime::now());
        let services = req
            .services
            .iter()
            .map(|service| {
                ServiceMetadata::new(
                    service.service_id.clone(),
                    req.cluster_id.clone(),
                    req.instance_id.clone(),
                    service.host.clone(),
                    service.port,
                    timestamp,
                )
            })
            .collect();
        NodeServiceMetadata::new(
            format!("{}/{}", req.cluster_id, req.instance_id),
            timestamp,
            services,
        )
    }
}
#[derive(Debug, Serialize, Deserialize)]
pub struct ServiceMetadata {
    service_id: String,
    cluster_id: String,
    instance_id: String,
    host: String,
    port: i32,
    timestamp: i64,
}

impl From<ServiceMetadata> for ServiceInstance {
    fn from(item: ServiceMetadata) -> Self {
        ServiceInstance {
            service_id: item.service_id,
            instance_id: item.instance_id,
            host: item.host,
            port: item.port,
        }
    }
}

impl ServiceMetadata {
    pub fn new(
        service_id: String,
        cluster_id: String,
        instance_id: String,
        host: String,
        port: i32,
        timestamp: i64,
    ) -> Self {
        Self {
            service_id,
            cluster_id,
            instance_id,
            host,
            port,
            timestamp,
        }
    }

    pub fn timestamp(&self) -> i64 {
        self.timestamp
    }

    pub fn port(&self) -> i32 {
        self.port
    }

    pub fn host(&self) -> &str {
        &self.host
    }

    pub fn instance_id(&self) -> &str {
        &self.instance_id
    }

    pub fn cluster_id(&self) -> &str {
        &self.cluster_id
    }

    pub fn service_id(&self) -> &str {
        &self.service_id
    }
}
