// This file is @generated by prost-build.
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct ServiceInstance {
    #[prost(string, tag = "1")]
    pub service_id: ::prost::alloc::string::String,
    #[prost(string, tag = "2")]
    pub cluster_id: ::prost::alloc::string::String,
    #[prost(string, tag = "3")]
    pub instance_id: ::prost::alloc::string::String,
    #[prost(string, tag = "4")]
    pub host: ::prost::alloc::string::String,
    #[prost(int32, tag = "5")]
    pub port: i32,
}
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord, ::prost::Enumeration)]
#[repr(i32)]
pub enum ServiceStatus {
    Unknown = 0,
    Active = 1,
    Dead = 2,
    Terminating = 3,
}
impl ServiceStatus {
    /// String value of the enum field names used in the ProtoBuf definition.
    ///
    /// The values are not transformed in any way and thus are considered stable
    /// (if the ProtoBuf definition does not change) and safe for programmatic use.
    pub fn as_str_name(&self) -> &'static str {
        match self {
            Self::Unknown => "UNKNOWN",
            Self::Active => "ACTIVE",
            Self::Dead => "DEAD",
            Self::Terminating => "TERMINATING",
        }
    }
    /// Creates an enum from field names used in the ProtoBuf definition.
    pub fn from_str_name(value: &str) -> ::core::option::Option<Self> {
        match value {
            "UNKNOWN" => Some(Self::Unknown),
            "ACTIVE" => Some(Self::Active),
            "DEAD" => Some(Self::Dead),
            "TERMINATING" => Some(Self::Terminating),
            _ => None,
        }
    }
}
