//! MySQL federated connector implementation.
//!
//! Provides MysqlFederatedConnector implementing the FederatedConnector trait
//! with connection pooling via mysql_async's built-in pool and type mapping via MysqlTypeAdapter.

use async_trait::async_trait;
use datafusion::arrow::array::{ArrayRef, StringBuilder};
use datafusion::arrow::datatypes::{DataType, Field, Schema};
use datafusion::arrow::record_batch::RecordBatch;
use datafusion::common::Result as DfResult;
use datafusion::physical_plan::{RecordBatchStream, SendableRecordBatchStream};
use futures::Stream;
use mysql_async::prelude::*;
use mysql_async::{Opts, Pool, Row};
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};

use crate::{Result, OctopusError};
use octopus_common::federated::{
    ConnectionPool as ConnectionPoolTrait, DatabaseType, FederatedConnector, PoolStats, TypeAdapter,
};

/// Configuration for MySQL connections.
#[derive(Debug, Clone)]
pub struct ConnectionConfig {
    pub host: String,
    pub port: u16,
    pub user: String,
    pub password: String,
    pub database: String,
    pub max_size: usize,
    pub timeout_secs: u64,
}

impl Default for ConnectionConfig {
    fn default() -> Self {
        Self {
            host: "localhost".to_string(),
            port: 3306,
            user: "root".to_string(),
            password: "".to_string(),
            database: "mysql".to_string(),
            max_size: 10,
            timeout_secs: 30,
        }
    }
}

impl ConnectionConfig {
    pub fn new(host: &str, port: u16, user: &str, password: &str, database: &str) -> Self {
        Self {
            host: host.to_string(),
            port,
            user: user.to_string(),
            password: password.to_string(),
            database: database.to_string(),
            ..Default::default()
        }
    }

    pub fn with_pool_size(mut self, max_size: usize) -> Self {
        self.max_size = max_size;
        self
    }

    pub fn with_timeout(mut self, timeout_secs: u64) -> Self {
        self.timeout_secs = timeout_secs;
        self
    }

    fn to_mysql_opts(&self) -> Opts {
        let url = if self.password.is_empty() {
            format!("mysql://{}@{}:{}/{}",
                self.user, self.host, self.port, self.database)
        } else {
            format!("mysql://{}:{}@{}:{}/{}",
                self.user, self.password, self.host, self.port, self.database)
        };
        Opts::from_url(&url).unwrap_or_default()
    }
}

/// MySQL type adapter for mapping between MySQL types and Arrow DataTypes.
#[derive(Clone)]
pub struct MysqlTypeAdapter;

impl MysqlTypeAdapter {
    pub fn new() -> Self {
        Self
    }
}

impl Default for MysqlTypeAdapter {
    fn default() -> Self {
        Self::new()
    }
}

impl TypeAdapter for MysqlTypeAdapter {
    fn database_type(&self) -> DatabaseType {
        DatabaseType::MySQL
    }

    fn to_arrow_type(&self, sql_type: &str) -> crate::Result<DataType> {
        let sql_type_upper = sql_type.to_uppercase();

        match sql_type_upper.as_str() {
            "VARCHAR" | "CHAR" | "TEXT" | "TINYTEXT" | "MEDIUMTEXT" | "LONGTEXT" | "ENUM" | "SET" => {
                Ok(DataType::Utf8)
            }
            "INT" | "INTEGER" | "SMALLINT" | "TINYINT" | "MEDIUMINT" => Ok(DataType::Int32),
            "BIGINT" => Ok(DataType::Int64),
            "FLOAT" | "FLOAT UNSIGNED" => Ok(DataType::Float32),
            "DOUBLE" | "DOUBLE UNSIGNED" | "DECIMAL" | "DECIMAL UNSIGNED" => Ok(DataType::Float64),
            "BIT" | "BOOL" | "BOOLEAN" => Ok(DataType::Boolean),
            "DATE" => Ok(DataType::Date32),
            "DATETIME" | "TIMESTAMP" => Ok(DataType::Timestamp(
                datafusion::arrow::datatypes::TimeUnit::Millisecond,
                None,
            )),
            "TIME" => Ok(DataType::Time64(datafusion::arrow::datatypes::TimeUnit::Microsecond)),
            "JSON" => Ok(DataType::LargeBinary),
            "BINARY" | "VARBINARY" | "BLOB" | "TINYBLOB" | "MEDIUMBLOB" | "LONGBLOB" => {
                Ok(DataType::LargeBinary)
            }
            "YEAR" => Ok(DataType::Int32),
            "INT UNSIGNED" | "INTEGER UNSIGNED" | "SMALLINT UNSIGNED" | "MEDIUMINT UNSIGNED" => {
                Ok(DataType::UInt32)
            }
            "BIGINT UNSIGNED" => Ok(DataType::UInt64),
            _ => {
                tracing::warn!(
                    "Unknown MySQL type: {}, defaulting to Utf8",
                    sql_type
                );
                Ok(DataType::Utf8)
            }
        }
    }

    fn from_arrow_type(&self, arrow_type: &DataType) -> crate::Result<String> {
        match arrow_type {
            DataType::Utf8 | DataType::LargeUtf8 => Ok("VARCHAR".to_string()),
            DataType::Int8 | DataType::UInt8 => Ok("TINYINT".to_string()),
            DataType::Int16 | DataType::UInt16 => Ok("SMALLINT".to_string()),
            DataType::Int32 | DataType::UInt32 => Ok("INT".to_string()),
            DataType::Int64 | DataType::UInt64 => Ok("BIGINT".to_string()),
            DataType::Float32 => Ok("FLOAT".to_string()),
            DataType::Float64 => Ok("DOUBLE".to_string()),
            DataType::Boolean => Ok("BOOLEAN".to_string()),
            DataType::Date32 => Ok("DATE".to_string()),
            DataType::Timestamp(_, None) => Ok("DATETIME".to_string()),
            DataType::Time64(_) => Ok("TIME".to_string()),
            DataType::Binary | DataType::LargeBinary => Ok("BLOB".to_string()),
            _ => Ok("TEXT".to_string()),
        }
    }
}

/// MySQL connection pool wrapper using mysql_async's built-in Pool.
pub struct MysqlConnectionPool {
    pool: Pool,
    config: ConnectionConfig,
}

impl MysqlConnectionPool {
    pub fn new(config: ConnectionConfig) -> Result<Self> {
        let opts = config.to_mysql_opts();
        let pool = Pool::new(opts);
        Ok(Self { pool, config })
    }

    pub fn from_config(config: ConnectionConfig) -> Result<Self> {
        Self::new(config)
    }

    /// Get a connection from the pool
    async fn get_conn(&self) -> crate::Result<mysql_async::Conn> {
        self.pool.get_conn().await.map_err(|e| {
            OctopusError::ConnectionPoolError(format!("Failed to get MySQL connection: {}", e))
        })
    }
}

#[async_trait]
impl ConnectionPoolTrait for MysqlConnectionPool {
    async fn get(&self) -> crate::Result<Box<dyn std::any::Any + Send>> {
        let conn = self.get_conn().await?;
        Ok(Box::new(conn))
    }

    async fn release(&self, conn: Box<dyn std::any::Any + Send>) -> crate::Result<()> {
        // mysql_async Pool automatically returns connection when it drops
        let _ = conn;
        Ok(())
    }

    fn stats(&self) -> PoolStats {
        PoolStats::new(
            self.config.max_size,
            0,
            0,
            0,
        )
    }
}

/// Helper struct to wrap a single record batch for streaming
struct SingleBatchStream {
    batch: Option<RecordBatch>,
}

impl Stream for SingleBatchStream {
    type Item = DfResult<RecordBatch>;

    fn poll_next(mut self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        Poll::Ready(self.batch.take().map(Ok))
    }
}

impl RecordBatchStream for SingleBatchStream {
    fn schema(&self) -> datafusion::arrow::datatypes::SchemaRef {
        self.batch
            .as_ref()
            .map(|b| b.schema())
            .unwrap_or_else(|| Arc::new(datafusion::arrow::datatypes::Schema::empty()))
    }
}

/// MySQL federated connector implementation.
pub struct MysqlFederatedConnector {
    pool: Arc<MysqlConnectionPool>,
    type_adapter: MysqlTypeAdapter,
    config: ConnectionConfig,
}

impl MysqlFederatedConnector {
    pub fn new(config: ConnectionConfig) -> Result<Self> {
        let pool = MysqlConnectionPool::new(config.clone())?;
        Ok(Self {
            pool: Arc::new(pool),
            type_adapter: MysqlTypeAdapter::new(),
            config,
        })
    }

    #[allow(dead_code)]
    pub fn default_config() -> Self {
        Self {
            pool: Arc::new(
                MysqlConnectionPool::new(ConnectionConfig::default())
                    .expect("Default config should be valid"),
            ),
            type_adapter: MysqlTypeAdapter::new(),
            config: ConnectionConfig::default(),
        }
    }
}

impl FederatedConnector for MysqlFederatedConnector {
    fn database_type(&self) -> DatabaseType {
        DatabaseType::MySQL
    }

    fn type_adapter(&self) -> Arc<dyn TypeAdapter> {
        Arc::new(self.type_adapter.clone())
    }

    fn connection_pool(&self) -> Arc<dyn ConnectionPoolTrait> {
        self.pool.clone()
    }

    fn get_schema(&self, table: &str) -> crate::Result<Schema> {
        tracing::info!("Getting schema for MySQL table: {}", table);
        Ok(Schema::new(vec![
            Field::new("column_name", DataType::Utf8, false),
            Field::new("data_type", DataType::Utf8, false),
            Field::new("is_nullable", DataType::Utf8, false),
        ]))
    }

    fn execute_query(&self, sql: &str) -> crate::Result<SendableRecordBatchStream> {
        tracing::info!("Executing MySQL query: {}", sql);

        let pool = self.pool.clone();

        let batch = futures::executor::block_on(async {
            let mut conn = pool.get_conn().await.map_err(|e| {
                OctopusError::ConnectionPoolError(format!("Failed to get connection: {}", e))
            })?;

            let rows: Vec<Row> = conn.query(sql).await.map_err(|e| {
                OctopusError::ExecutionError(format!("Query failed: {}", e))
            })?;

            if let Some(row) = rows.first() {
                let num_cols = row.len();
                let mut builders: Vec<Box<dyn datafusion::arrow::array::ArrayBuilder>> =
                    (0..num_cols)
                        .map(|_| {
                            Box::new(StringBuilder::new())
                                as Box<dyn datafusion::arrow::array::ArrayBuilder>
                        })
                        .collect();

                for row in &rows {
                    for i in 0..num_cols {
                        let value: Option<String> = row.get::<String, _>(i);
                        if let Some(v) = value {
                            if let Some(builder) =
                                builders[i].as_any_mut().downcast_mut::<StringBuilder>()
                            {
                                let _ = builder.append_value(&v);
                            }
                        } else {
                            if let Some(builder) =
                                builders[i].as_any_mut().downcast_mut::<StringBuilder>()
                            {
                                let _ = builder.append_null();
                            }
                        }
                    }
                }

                let arrays: Vec<ArrayRef> =
                    builders.into_iter().map(|mut b| b.finish()).collect();

                let fields: Vec<Field> = (0..num_cols)
                    .map(|i| Field::new(format!("col_{}", i), DataType::Utf8, true))
                    .collect();
                let schema = Arc::new(Schema::new(fields));

                RecordBatch::try_new(schema, arrays)
                    .map_err(|e| OctopusError::ExecutionError(format!("Record batch error: {}", e)))
            } else {
                let schema = Arc::new(Schema::new(vec![Field::new("empty", DataType::Utf8, true)]));
                RecordBatch::try_new(schema, vec![])
                    .map_err(|e| OctopusError::ExecutionError(format!("Record batch error: {}", e)))
            }
        })?;

        let stream = SingleBatchStream { batch: Some(batch) };
        Ok(Box::pin(stream))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_mysql_type_adapter_varchar() {
        let adapter = MysqlTypeAdapter::new();
        let result = adapter.to_arrow_type("VARCHAR");
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), DataType::Utf8);
    }

    #[test]
    fn test_mysql_type_adapter_integer() {
        let adapter = MysqlTypeAdapter::new();
        let result = adapter.to_arrow_type("INTEGER");
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), DataType::Int32);
    }

    #[test]
    fn test_mysql_type_adapter_bigint() {
        let adapter = MysqlTypeAdapter::new();
        let result = adapter.to_arrow_type("BIGINT");
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), DataType::Int64);
    }

    #[test]
    fn test_mysql_type_adapter_datetime() {
        let adapter = MysqlTypeAdapter::new();
        let result = adapter.to_arrow_type("DATETIME");
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), DataType::Timestamp(
            datafusion::arrow::datatypes::TimeUnit::Millisecond,
            None,
        ));
    }

    #[test]
    fn test_connection_config_default() {
        let config = ConnectionConfig::default();
        assert_eq!(config.host, "localhost");
        assert_eq!(config.port, 3306);
        assert_eq!(config.max_size, 10);
    }

    #[test]
    fn test_connection_config_builder() {
        let config = ConnectionConfig::new("db.example.com", 3307, "user", "pass", "mydb")
            .with_pool_size(20)
            .with_timeout(60);

        assert_eq!(config.host, "db.example.com");
        assert_eq!(config.port, 3307);
        assert_eq!(config.user, "user");
        assert_eq!(config.database, "mydb");
        assert_eq!(config.max_size, 20);
        assert_eq!(config.timeout_secs, 60);
    }
}
