pub mod error;
pub mod federated;
pub mod udf;

pub use error::OctopusError;
pub use federated::{
    DatabaseType, FederatedConnector, TypeAdapter, ConnectionPool,
    PoolStats,
};

pub type Result<T> = std::result::Result<T, OctopusError>;
