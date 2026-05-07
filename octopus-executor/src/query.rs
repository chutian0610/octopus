use super::session::ExecutorSession;
use crate::{OctopusError, Result};
use std::sync::Arc;
use tracing::{info, instrument};

pub struct QueryExecutor {
    session: Arc<ExecutorSession>,
}

impl QueryExecutor {
    pub fn new(session: Arc<ExecutorSession>) -> Self {
        Self { session }
    }

    #[instrument(skip(self, sql))]
    pub async fn execute_sql(
        &self,
        sql: &str,
    ) -> Result<Vec<datafusion::arrow::record_batch::RecordBatch>> {
        info!("Executing SQL: {}", sql);

        let context = self.session.context();

        let df = context
            .sql(sql)
            .await
            .map_err(|e| OctopusError::SqlError(e.to_string()))?;

        let results = df
            .collect()
            .await
            .map_err(|e| OctopusError::ExecutionError(e.to_string()))?;

        info!("Query returned {} batches", results.len());
        Ok(results)
    }

    pub async fn execute_sql_json(&self, sql: &str) -> Result<String> {
        let batches = self.execute_sql(sql).await?;

        let mut rows: Vec<String> = Vec::new();
        for batch in batches {
            let num_rows = batch.num_rows();
            let num_cols = batch.num_columns();

            for _row_idx in 0..num_rows {
                let mut row_values: Vec<String> = Vec::new();
                for col_idx in 0..num_cols {
                    let array = batch.column(col_idx);
                    let val_str = format!("{:?}", array);
                    row_values.push(val_str);
                }
                rows.push(format!("{{{}}}", row_values.join(", ")));
            }
        }

        Ok(format!("[{}]", rows.join(", ")))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_simple_select() {
        let session = ExecutorSession::new().unwrap();
        let ctx = session.context();
        ctx.sql("CREATE TABLE test AS VALUES (1, 'a'), (2, 'b'), (3, 'c')")
            .await
            .unwrap();

        let executor = QueryExecutor::new(Arc::new(session));

        let results = executor
            .execute_sql("SELECT * FROM test WHERE column1 > 1")
            .await
            .unwrap();
        assert!(!results.is_empty());
    }
}
