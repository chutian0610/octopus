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

pub mod discovery_server;
pub mod discovery_client;
mod discovery_state;

pub struct ServiceInstance {

}