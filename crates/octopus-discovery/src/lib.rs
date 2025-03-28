use std::time::SystemTime;

use octopus_common::time_util::to_millis;
use octopus_rpc::{common::Entry, discovery::NodeAnnounceReq};
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

    fn to_entry(&self) -> Entry {
        let key = self.node_id.as_bytes().to_vec();
        let value = serde_json::to_vec(self).unwrap();
        let version = self.timestamp;
        Entry {
            key,
            value,
            version,
        }
    }

    fn from_node_announce_request(req: &NodeAnnounceReq) -> Self {
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
        Self::new(
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
