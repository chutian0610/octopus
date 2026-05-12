//! PostgreSQL federated connector implementation.
//!
//! Provides PostgresFederatedConnector implementing the FederatedConnector trait
//! with connection pooling via deadpool-postgres and type mapping via PostgresTypeAdapter.

use async_trait::async_trait;
use arrow::datatypes::TimeUnit;
use datafusion::arrow::array::{ArrayRef, StringBuilder};
use datafusion::arrow::datatypes::{DataType, Field, Schema};
use datafusion::arrow::error::ArrowError;
use datafusion::arrow::record_batch::RecordBatch;
use datafusion::physical_plan::{RecordBatchStream, SendableRecordBatchStream};
use deadpool_postgres::{Manager, ManagerConfig, Pool, RecyclingMethod, Runtime, Object};
use futures::{Stream, StreamExt};
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};
use tokio_postgres::NoTls;

use crate::{Result, OctopusError};
use octopus_common::federated::{
    ConnectionPool as ConnectionPoolTrait, DatabaseType, FederatedConnector, PoolStats, TypeAdapter,
};

/// Configuration for PostgreSQL connections.
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
            port: 5432,
            user: "postgres".to_string(),
            password: "".to_string(),
            database: "postgres".to_string(),
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
}

/// PostgreSQL type adapter for mapping between PostgreSQL types and Arrow DataTypes.
#[derive(Clone)]
pub struct PostgresTypeAdapter;

impl PostgresTypeAdapter {
    pub fn new() -> Self {
        Self
    }
}

impl Default for PostgresTypeAdapter {
    fn default() -> Self {
        Self::new()
    }
}

impl TypeAdapter for PostgresTypeAdapter {
    fn database_type(&self) -> DatabaseType {
        DatabaseType::PostgreSQL
    }

    fn to_arrow_type(&self, sql_type: &str) -> crate::Result<DataType> {
        let sql_type_upper = sql_type.to_uppercase();
        let sql_type_clean: &str = &sql_type_upper.trim_start_matches('_');

        match sql_type_clean {
            "VARCHAR" | "CHARACTER VARYING" | "CHARACTER" | "TEXT" | "NAME" | "bpchar" | "string" => {
                Ok(DataType::Utf8)
            }
            "INTEGER" | "INT" | "INT4" | "SERIAL" | "OID" => Ok(DataType::Int32),
            "BIGINT" | "INT8" | "BIGSERIAL" => Ok(DataType::Int64),
            "SMALLINT" | "INT2" | "SMALLSERIAL" => Ok(DataType::Int16),
            "REAL" | "FLOAT4" | "FLOAT" => Ok(DataType::Float32),
            "DOUBLE PRECISION" | "FLOAT8" => Ok(DataType::Float64),
            "BOOLEAN" | "BOOL" => Ok(DataType::Boolean),
            "DATE" => Ok(DataType::Date32),
            "TIME" | "TIME WITHOUT TIME ZONE" => {
                Ok(DataType::Time64(TimeUnit::Microsecond))
            }
            "TIMESTAMP" | "TIMESTAMP WITHOUT TIME ZONE" => Ok(DataType::Timestamp(
                datafusion::arrow::datatypes::TimeUnit::Millisecond,
                None,
            )),
            "TIMESTAMPTZ" | "TIMESTAMP WITH TIME ZONE" => Ok(DataType::Timestamp(
                datafusion::arrow::datatypes::TimeUnit::Millisecond,
                Some("UTC".into()),
            )),
            "UUID" => Ok(DataType::LargeBinary),
            "JSON" | "JSONB" => Ok(DataType::LargeBinary),
            "BYTEA" => Ok(DataType::Binary),
            "ARRAY" | "_text" | "_varchar" | "_int4" | "_int8" | "_float4" | "_float8" => {
                Ok(DataType::LargeList(std::sync::Arc::new(datafusion::arrow::datatypes::Field::new(
                    "item",
                    DataType::Utf8,
                    true,
                ))))
            }
            "INTERVAL" => Ok(DataType::Interval(
                datafusion::arrow::datatypes::IntervalUnit::DayTime,
            )),
            "MACADDR" | "INET" | "CIDR" => Ok(DataType::Utf8),
            _ => {
                tracing::warn!(
                    "Unknown PostgreSQL type: {}, defaulting to Utf8",
                    sql_type
                );
                Ok(DataType::Utf8)
            }
        }
    }

    fn from_arrow_type(&self, arrow_type: &DataType) -> crate::Result<String> {
        match arrow_type {
            DataType::Utf8 | DataType::LargeUtf8 | DataType::FixedSizeBinary(_) => {
                Ok("VARCHAR".to_string())
            }
            DataType::Int8 | DataType::Int16 | DataType::UInt8 | DataType::UInt16 => {
                Ok("SMALLINT".to_string())
            }
            DataType::Int32 | DataType::UInt32 => Ok("INTEGER".to_string()),
            DataType::Int64 | DataType::UInt64 => Ok("BIGINT".to_string()),
            DataType::Float32 => Ok("REAL".to_string()),
            DataType::Float64 => Ok("DOUBLE PRECISION".to_string()),
            DataType::Boolean => Ok("BOOLEAN".to_string()),
            DataType::Date32 => Ok("DATE".to_string()),
            DataType::Date64 | DataType::Timestamp(_, None) => Ok("TIMESTAMP".to_string()),
            DataType::Timestamp(_, Some(_)) => Ok("TIMESTAMPTZ".to_string()),
            DataType::Time64(_) => Ok("TIME".to_string()),
            DataType::Binary | DataType::LargeBinary => Ok("BYTEA".to_string()),
            _ => Ok("TEXT".to_string()),
        }
    }
}

/// PostgreSQL connection pool wrapper using deadpool-postgres.
pub struct PostgresConnectionPool {
    pool: Pool,
    config: ConnectionConfig,
}

impl PostgresConnectionPool {
    pub fn new(config: ConnectionConfig) -> Result<Self> {
        let mut pg_config = tokio_postgres::Config::new();
        pg_config.host(&config.host);
        pg_config.port(config.port);
        pg_config.user(&config.user);
        pg_config.password(&config.password);
        pg_config.dbname(&config.database);

        let mgr_config = ManagerConfig {
            recycling_method: RecyclingMethod::Fast,
        };

        let mgr = Manager::from_config(pg_config, NoTls, mgr_config);

        let pool = Pool::builder(mgr)
            .max_size(config.max_size)
            .runtime(Runtime::Tokio1)
            .build()
            .map_err(|e| OctopusError::ConnectionPoolError(e.to_string()))?;

        Ok(Self { pool, config })
    }

    pub fn from_config(config: ConnectionConfig) -> Result<Self> {
        Self::new(config)
    }
}

#[async_trait]
impl ConnectionPoolTrait for PostgresConnectionPool {
    async fn get(&self) -> crate::Result<Box<dyn std::any::Any + Send>> {
        let conn = self.pool.get().await.map_err(|e| {
            OctopusError::ConnectionPoolError(format!("Failed to get connection: {}", e))
        })?;
        Ok(Box::new(conn))
    }

    async fn release(&self, conn: Box<dyn std::any::Any + Send>) -> crate::Result<()> {
        let _ = conn;
        Ok(())
    }

    fn stats(&self) -> PoolStats {
        PoolStats::new(self.config.max_size, 0, 0, 0)
    }
}

/// Helper struct to wrap a single record batch for streaming
struct SingleBatchStream {
    batch: Option<RecordBatch>,
}

impl Stream for SingleBatchStream {
    type Item = datafusion::common::Result<RecordBatch>;

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

/// PostgreSQL federated connector implementation.
pub struct PostgresFederatedConnector {
    pool: Arc<PostgresConnectionPool>,
    type_adapter: PostgresTypeAdapter,
    config: ConnectionConfig,
}

impl PostgresFederatedConnector {
    pub fn new(config: ConnectionConfig) -> Result<Self> {
        let pool = PostgresConnectionPool::new(config.clone())?;
        Ok(Self {
            pool: Arc::new(pool),
            type_adapter: PostgresTypeAdapter::new(),
            config,
        })
    }

    #[allow(dead_code)]
    pub fn default_config() -> Self {
        Self {
            pool: Arc::new(
                PostgresConnectionPool::new(ConnectionConfig::default())
                    .expect("Default config should be valid"),
            ),
            type_adapter: PostgresTypeAdapter::new(),
            config: ConnectionConfig::default(),
        }
    }
}

impl FederatedConnector for PostgresFederatedConnector {
    fn database_type(&self) -> DatabaseType {
        DatabaseType::PostgreSQL
    }

    fn type_adapter(&self) -> Arc<dyn TypeAdapter> {
        Arc::new(self.type_adapter.clone())
    }

    fn connection_pool(&self) -> Arc<dyn ConnectionPoolTrait> {
        self.pool.clone()
    }

    fn get_schema(&self, table: &str) -> crate::Result<Schema> {
        tracing::info!("Getting schema for table: {}", table);
        Ok(Schema::new(vec![
            Field::new("column_name", DataType::Utf8, false),
            Field::new("data_type", DataType::Utf8, false),
            Field::new("is_nullable", DataType::Utf8, false),
        ]))
    }

    fn execute_query(&self, sql: &str) -> crate::Result<SendableRecordBatchStream> {
        tracing::info!("Executing PostgreSQL query: {}", sql);

        let pool = self.pool.clone();

        let batch = futures::executor::block_on(async {
            let conn_box = pool.get().await.map_err(|e| {
                OctopusError::ConnectionPoolError(format!("Failed to get connection: {}", e))
            })?;

            let conn = conn_box.downcast_ref::<Object>().ok_or_else(|| {
                OctopusError::ExecutionError("Failed to downcast connection".to_string())
            })?;

            let rows = conn.query(sql, &[]).await.map_err(|e| {
                OctopusError::ExecutionError(format!("Query failed: {}", e))
            })?;

            if let Some(row) = rows.first() {
                let num_cols = row.columns().len();
                let mut builders: Vec<Box<dyn datafusion::arrow::array::ArrayBuilder>> =
                    (0..num_cols)
                        .map(|_| {
                            Box::new(StringBuilder::new())
                                as Box<dyn datafusion::arrow::array::ArrayBuilder>
                        })
                        .collect();

                for row in &rows {
                    for i in 0..num_cols {
                        let value: Option<String> = row.get(i);
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

                let fields: Vec<Field> = row.columns()
                    .iter()
                    .map(|c| Field::new(c.name(), DataType::Utf8, true))
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
    fn test_postgres_type_adapter_varchar() {
        let adapter = PostgresTypeAdapter::new();
        let result = adapter.to_arrow_type("VARCHAR");
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), DataType::Utf8);
    }

    #[test]
    fn test_postgres_type_adapter_integer() {
        let adapter = PostgresTypeAdapter::new();
        let result = adapter.to_arrow_type("INTEGER");
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), DataType::Int32);
    }

    #[test]
    fn test_postgres_type_adapter_bigint() {
        let adapter = PostgresTypeAdapter::new();
        let result = adapter.to_arrow_type("BIGINT");
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), DataType::Int64);
    }

    #[test]
    fn test_postgres_type_adapter_uuid() {
        let adapter = PostgresTypeAdapter::new();
        let result = adapter.to_arrow_type("UUID");
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), DataType::LargeBinary);
    }

    #[test]
    fn test_connection_config_default() {
        let config = ConnectionConfig::default();
        assert_eq!(config.host, "localhost");
        assert_eq!(config.port, 5432);
        assert_eq!(config.max_size, 10);
    }

    #[test]
    fn test_connection_config_builder() {
        let config = ConnectionConfig::new("db.example.com", 5433, "user", "pass", "mydb")
            .with_pool_size(20)
            .with_timeout(60);

        assert_eq!(config.host, "db.example.com");
        assert_eq!(config.port, 5433);
        assert_eq!(config.user, "user");
        assert_eq!(config.database, "mydb");
        assert_eq!(config.max_size, 20);
        assert_eq!(config.timeout_secs, 60);
    }
}
