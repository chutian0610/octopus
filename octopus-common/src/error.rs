use thiserror::Error;

#[derive(Error, Debug)]
pub enum OctopusError {
    #[error("SQL execution error: {0}")]
    SqlError(String),

    #[error("Data source error: {0}")]
    DataSourceError(String),

    #[error("Execution error: {0}")]
    ExecutionError(String),

    #[error("Parse error: {0}")]
    ParseError(String),

    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),

    #[error("Object store error: {0}")]
    ObjectStoreError(String),
}
