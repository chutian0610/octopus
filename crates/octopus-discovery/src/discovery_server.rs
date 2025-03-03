use octopus_rpc::discovery::{discovery_service_server::DiscoveryService, LookUpReq, LookUpResp, SerivceAnnounceReq, ServiceAnnounceResp};
use tonic::{Request, Response, Status};
#[derive(Clone)]
pub struct DiscoveryServer{
    
}
impl DiscoveryServer {
    pub fn new() -> Self {
        DiscoveryServer {}
    }
}

#[tonic::async_trait]
impl DiscoveryService for DiscoveryServer {
    async fn announce(
        &self,
        request: Request<SerivceAnnounceReq>,
    ) -> Result<Response<ServiceAnnounceResp>,Status>{
        let req = request.into_inner();
        
        todo!()
    }
    async fn un_announce(
        &self,
        request: Request<SerivceAnnounceReq>,
    ) -> Result<Response<ServiceAnnounceResp>,Status>{
        todo!()
    }
    async fn look_up(
        &self,
        request: Request<LookUpReq>,
    ) -> Result<Response<LookUpResp>,Status>{
        todo!()
    }
}