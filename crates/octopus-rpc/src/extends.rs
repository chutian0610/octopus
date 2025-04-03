use crate::{
    common::{NodeEntry, NodeMetadata, ServiceMetadata},
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
    a: Option<&'a NodeEntry>,
    b: &'a NodeEntry,
) -> &'a NodeEntry {
    if a.is_none() {
        return b;
    }
    resolve_node_entry(a.unwrap(), b)
}
pub fn resolve_node_entry<'a>(a: &'a NodeEntry, b: &'a NodeEntry) -> &'a NodeEntry {
    if a.timestamp > b.timestamp {
        a
    } else if a.timestamp < b.timestamp {
        b
    } else {
        a
    }
}
impl NodeEntry {
    pub fn from_register_node_metadata(value: &NodeMetadata) -> Self {
        NodeEntry {
            node_id: value.node_id.clone(),
            meta: Option::Some(value.clone()),
            timestamp: value.timestamp,
        }
    }
    pub fn from_unregister_node_metadata(value: &NodeMetadata) -> Self {
        NodeEntry {
            node_id: value.node_id.clone(),
            meta: Option::None,
            timestamp: value.timestamp,
        }
    }
}
