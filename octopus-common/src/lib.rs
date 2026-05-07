pub mod error;

pub use error::OctopusError;

pub type Result<T> = std::result::Result<T, OctopusError>;
