//! Federated connector traits for database adapters and connection pooling.
//!
//! This module provides the foundation for federated queries against PostgreSQL
//! and MySQL databases. It follows the adapter pattern (D-04) with per-database
//! type adapters and connection pooling per worker (D-01).

use async_trait::async_trait;
use datafusion::arrow::datatypes::{DataType, Field, Schema};
use datafusion::physical_plan::SendableRecordBatchStream;
use std::sync::Arc;

/// Supported database types for federated queries.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DatabaseType {
    PostgreSQL,
    MySQL,
}

impl DatabaseType {
    pub fn as_str(&self) -> &'static str {
        match self {
            DatabaseType::PostgreSQL => "postgresql",
            DatabaseType::MySQL => "mysql",
        }
    }
}

/// Statistics for connection pool monitoring.
#[derive(Debug, Clone, Default)]
pub struct PoolStats {
    pub total_connections: usize,
    pub idle_connections: usize,
    pub used_connections: usize,
    pub waiting_tasks: usize,
}

impl PoolStats {
    pub fn new(total: usize, idle: usize, used: usize, waiting: usize) -> Self {
        Self {
            total_connections: total,
            idle_connections: idle,
            used_connections: used,
            waiting_tasks: waiting,
        }
    }
}

/// TypeAdapter trait for mapping between database-specific types and Arrow types.
///
/// Each database adapter (PostgreSQL, MySQL) implements this trait to handle
/// database-specific type systems. For example, PostgreSQL's UUID, JSONB, ARRAY
/// vs MySQL's DATETIME, JSON.
#[async_trait]
pub trait TypeAdapter: Send + Sync {
    /// Returns the database type this adapter handles.
    fn database_type(&self) -> DatabaseType;

    /// Converts a database SQL type string to an Arrow DataType.
    fn to_arrow_type(&self, sql_type: &str) -> crate::Result<DataType>;

    /// Converts an Arrow DataType back to a database-specific SQL type string.
    fn from_arrow_type(&self, arrow_type: &DataType) -> crate::Result<String>;

    /// Maps a database column to an Arrow Field.
    fn to_arrow_field(&self, column_name: &str, sql_type: &str, nullable: bool) -> crate::Result<Field> {
        let data_type = self.to_arrow_type(sql_type)?;
        Ok(Field::new(column_name, data_type, nullable))
    }
}

/// ConnectionPool trait for abstracting async connection pool operations.
///
/// Supports retrieval and release of database connections with statistics tracking.
/// Uses type-erased connections (Box<dyn Any + Send>) to allow trait objects.
#[async_trait]
pub trait ConnectionPool: Send + Sync {
    /// Acquires a connection from the pool.
    async fn get(&self) -> crate::Result<Box<dyn std::any::Any + Send>>;

    /// Releases a connection back to the pool.
    async fn release(&self, conn: Box<dyn std::any::Any + Send>) -> crate::Result<()>;

    /// Returns current pool statistics.
    fn stats(&self) -> PoolStats;
}

/// FederatedConnector trait — main interface for federated data sources.
///
/// Implementations provide connection pooling, type mapping, and query execution
/// for specific database types. Each connector maintains its own connection pool
/// per worker (D-01 decision).
pub trait FederatedConnector: Send + Sync {
    /// Returns the type of database this connector targets.
    fn database_type(&self) -> DatabaseType;

    /// Returns the type adapter for this connector.
    fn type_adapter(&self) -> Arc<dyn TypeAdapter>;

    /// Returns the connection pool for this connector.
    fn connection_pool(&self) -> Arc<dyn ConnectionPool>;

    /// Retrieves the schema for a table in the federated database.
    fn get_schema(&self, table: &str) -> crate::Result<Schema>;

    /// Executes a SQL query and returns results as an Arrow record batch stream.
    fn execute_query(&self, sql: &str) -> crate::Result<SendableRecordBatchStream>;
}
