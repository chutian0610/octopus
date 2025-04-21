use crate::{
    common::{NodeMetadata, ServiceMetadata},
    discovery::NodeAnnounceReq,
};

impl From<&NodeAnnounceReq> for NodeMetadata {
    fn from(req: &NodeAnnounceReq) -> Self {
        let services = req
            .services
            .iter()
            .map(|service| ServiceMetadata {
                service_id: service.service_id.clone(),
                cluster_id: req.cluster_id.clone(),
                instance_id: req.instance_id.clone(),
                host: service.host.clone(),
                port: service.port,
                timestamp: req.timestamp,
                metadata: service.metadata.clone(),
            })
            .collect();
        NodeMetadata {
            node_id: format!("{}/{}", req.cluster_id, req.instance_id),
            services,
            timestamp: req.timestamp,
        }
    }
}

/// resolve the entry with the new entry.
pub fn resolve_optional_node_entry<'a>(
    a: Option<&'a NodeMetadata>,
    b: &'a NodeMetadata,
) -> &'a NodeMetadata {
    if a.is_none() {
        return b;
    }
    resolve_node_entry(a.unwrap(), b)
}
pub fn resolve_node_entry<'a>(a: &'a NodeMetadata, b: &'a NodeMetadata) -> &'a NodeMetadata {
    if a.timestamp > b.timestamp {
        a
    } else if a.timestamp < b.timestamp {
        b
    } else {
        a
    }
}
impl NodeMetadata {
    pub fn to_unregister_node_metadata(value: &NodeMetadata) -> Self {
        NodeMetadata {
            node_id: value.node_id.clone(),
            services: Vec::new(),
            timestamp: value.timestamp,
        }
    }
}
