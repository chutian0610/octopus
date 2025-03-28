use std::sync::Arc;

use octopus_rpc::discovery::{
    discovery_service_server::DiscoveryService, LookUpReq, LookUpResp, NodeAnnounceReq,
    NodeAnnounceResp,
};
use tonic::{Request, Response, Status};

use crate::{discovery_state::DiscoveryState, NodeServiceMetadata};

pub struct DiscoveryServer {
    state: Box<dyn DiscoveryState>,
}

impl DiscoveryServer {
    pub fn new(state: Box<dyn DiscoveryState>) -> Self {
        Self { state }
    }
}

#[tonic::async_trait]
impl DiscoveryService for DiscoveryServer {
    async fn announce(
        &self,
        request: Request<NodeAnnounceReq>,
    ) -> Result<Response<NodeAnnounceResp>, Status> {
        let req = request.into_inner();
        let result = self
            .state
            .save(NodeServiceMetadata::from_node_announce_request(&req))
            .await;
        match result {
            Ok(_) => Ok(Response::new(NodeAnnounceResp {})),
            Err(e) => {}
        }
        todo!()
    }
    async fn un_announce(
        &self,
        request: Request<NodeAnnounceReq>,
    ) -> Result<Response<NodeAnnounceResp>, Status> {
        todo!()
    }
    async fn look_up(&self, request: Request<LookUpReq>) -> Result<Response<LookUpResp>, Status> {
        todo!()
    }
}
