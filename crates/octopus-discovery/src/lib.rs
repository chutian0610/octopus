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
#[warn(dead_code)]
pub mod discovery_server;
mod discovery_state;
