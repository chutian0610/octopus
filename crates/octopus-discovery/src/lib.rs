///           ┌─────────────┐     
///           │             │     
///    ┌──────┼  Discovery  │     
///    │      │    Server   │     
///    │      │             │     
///    │      └────────▲────┘     
///    │               │          
///    │LookUp         │Announce  
///    │               │UnAnnounce
///    │               │          
/// ┌──▼──────────┐    │          
/// │             │    │          
/// │  Discovery  ┼────┘          
/// │    Client   │               
/// │             │               
/// └─────────────┘  
pub mod discovery_client;             
pub mod discovery_server;

mod discovery_state;
#[derive(Clone)] 
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
