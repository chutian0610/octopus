use std::sync::Arc;

use crate::{discovery_state::DiscoveryState, NodeServiceMetadata};
use octopus_rpc::{
    common::ServiceInstance,
    discovery::{
        discovery_service_server::DiscoveryService, LookUpReq, LookUpResp, NodeAnnounceReq,
        NodeAnnounceResp,
    },
};
use tonic::{Request, Response, Status};

pub struct DiscoveryServer {
    state: Arc<dyn DiscoveryState>,
}

impl DiscoveryServer {
    pub fn new(state: Arc<dyn DiscoveryState>) -> Self {
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
        let result = self.state.save(&NodeServiceMetadata::from(&req)).await;
        match result {
            Ok(_) => Ok(Response::new(NodeAnnounceResp {})),
            Err(e) => Err(Status::internal(format!("Node Announce failed: {}", e))),
        }
    }
    async fn un_announce(
        &self,
        request: Request<NodeAnnounceReq>,
    ) -> Result<Response<NodeAnnounceResp>, Status> {
        let req = request.into_inner();
        let result = self.state.remove(&NodeServiceMetadata::from(&req)).await;
        match result {
            Ok(_) => Ok(Response::new(NodeAnnounceResp {})),
            Err(e) => Err(Status::internal(format!("Node UnAnnounce failed: {}", e))),
        }
    }
    async fn look_up(&self, request: Request<LookUpReq>) -> Result<Response<LookUpResp>, Status> {
        fn option_str(a: Option<&String>, b: Option<&String>) -> String {
            if a.is_none() && b.is_none() {
                return String::from("ALL");
            }
            if a.is_none() {
                return b.unwrap().to_string();
            }
            if b.is_none() {
                return a.unwrap().to_string();
            }
            return format!("{},{}", a.unwrap(), b.unwrap());
        }

        let req = request.into_inner();
        let result = self
            .state
            .list(
                req.service_id.as_ref().map(|s| s.as_str()),
                req.cluster_id.as_ref().map(|s| s.as_str()),
            )
            .await;
        match result {
            Ok(value) => Ok(Response::new(LookUpResp {
                instances: value
                    .into_iter()
                    .map(|s| ServiceInstance::from(s))
                    .collect(),
            })),
            Err(e) => Err(Status::internal(format!(
                "look up services with option[{}] failed: {}",
                option_str(req.service_id.as_ref(), req.cluster_id.as_ref()),
                e
            ))),
        }
    }
}
