//! Octopus JDBC ResultSet implementation

use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jint, jlong, jstring, JNI_TRUE};
use jni::JNIEnv;
use std::sync::Arc;
use crate::connection::QueryResult;

/// Octopus JDBC ResultSet
///
/// Provides access to query results with iteration and column access.
pub struct OctopusResultSet {
    result: QueryResult,
    current_row: usize,
    closed: bool,
}

impl OctopusResultSet {
    pub fn new(result: QueryResult) -> Self {
        Self {
            result,
            current_row: 0,
            closed: false,
        }
    }

    /// Move to next row
    pub fn next(&mut self) -> bool {
        if self.closed {
            return false;
        }
        if self.current_row < self.result.rows.len() {
            self.current_row += 1;
            true
        } else {
            false
        }
    }

    /// Get string value by column index (1-based)
    pub fn get_string(&self, column_index: usize) -> Option<String> {
        self.get_current_row()?
            .get(self.result.schema.get(column_index - 1)?)
            .and_then(|v| v.as_str().map(|s| s.to_string()))
    }

    /// Get string value by column name
    pub fn get_string_by_name(&self, column_name: &str) -> Option<String> {
        self.get_current_row()?
            .get(column_name)
            .and_then(|v| v.as_str().map(|s| s.to_string()))
    }

    /// Get integer value by column index (1-based)
    pub fn get_int(&self, column_index: usize) -> Option<i32> {
        self.get_current_row()?
            .get(self.result.schema.get(column_index - 1)?)
            .and_then(|v| v.as_i64())
            .map(|v| v as i32)
    }

    /// Get integer value by column name
    pub fn get_int_by_name(&self, column_name: &str) -> Option<i32> {
        self.get_current_row()?
            .get(column_name)
            .and_then(|v| v.as_i64())
            .map(|v| v as i32)
    }

    /// Get double value by column index (1-based)
    pub fn get_double(&self, column_index: usize) -> Option<f64> {
        self.get_current_row()?
            .get(self.result.schema.get(column_index - 1)?)
            .and_then(|v| v.as_f64())
    }

    /// Get double value by column name
    pub fn get_double_by_name(&self, column_name: &str) -> Option<f64> {
        self.get_current_row()?
            .get(column_name)
            .and_then(|v| v.as_f64())
    }

    /// Get timestamp value by column index (1-based)
    pub fn get_timestamp(&self, column_index: usize) -> Option<i64> {
        self.get_current_row()?
            .get(self.result.schema.get(column_index - 1)?)
            .and_then(|v| v.as_i64())
    }

    /// Get timestamp value by column name
    pub fn get_timestamp_by_name(&self, column_name: &str) -> Option<i64> {
        self.get_current_row()?
            .get(column_name)
            .and_then(|v| v.as_i64())
    }

    /// Get column count
    pub fn get_column_count(&self) -> usize {
        self.result.schema.len()
    }

    /// Get column name by index (1-based)
    pub fn get_column_name(&self, column_index: usize) -> Option<&str> {
        self.result.schema.get(column_index - 1).map(|s| s.as_str())
    }

    /// Get column type name by index (1-based)
    pub fn get_column_type_name(&self, column_index: usize) -> String {
        self.get_current_row()
            .and_then(|row| row.get(self.result.schema.get(column_index - 1)?))
            .map(|v| match v {
                serde_json::Value::Null => "NULL".to_string(),
                serde_json::Value::Bool(_) => "BOOLEAN".to_string(),
                serde_json::Value::Number(_) => "DOUBLE".to_string(),
                serde_json::Value::String(_) => "VARCHAR".to_string(),
                _ => "VARCHAR".to_string(),
            })
            .unwrap_or_else(|| "UNKNOWN".to_string())
    }

    fn get_current_row(&self) -> Option<&serde_json::Map<String, serde_json::Value>> {
        if self.current_row > 0 && self.current_row <= self.result.rows.len() {
            self.result.rows.get(self.current_row - 1)
        } else {
            None
        }
    }

    pub fn is_closed(&self) -> bool {
        self.closed
    }

    pub fn close(&mut self) {
        self.closed = true;
    }
}

/// ResultSet metadata for BI tool introspection
pub struct ResultSetMetaData {
    column_count: usize,
    column_names: Vec<String>,
    column_types: Vec<String>,
}

impl ResultSetMetaData {
    pub fn new(schema: &[String], rows: &[serde_json::Map<String, serde_json::Value>]) -> Self {
        let column_types = if let Some(first_row) = rows.first() {
            schema.iter().map(|name| {
                first_row.get(name).map(|v| match v {
                    serde_json::Value::Null => "NULL".to_string(),
                    serde_json::Value::Bool(_) => "BOOLEAN".to_string(),
                    serde_json::Value::Number(_) => "DOUBLE".to_string(),
                    serde_json::Value::String(_) => "VARCHAR".to_string(),
                    _ => "VARCHAR".to_string(),
                }).unwrap_or_else(|| "UNKNOWN".to_string())
            }).collect()
        } else {
            vec!["UNKNOWN".to_string(); schema.len()]
        };

        Self {
            column_count: schema.len(),
            column_names: schema.to_vec(),
            column_types,
        }
    }

    pub fn get_column_count(&self) -> usize {
        self.column_count
    }

    pub fn get_column_label(&self, index: usize) -> Option<&str> {
        self.column_names.get(index).map(|s| s.as_str())
    }

    pub fn get_column_type_name(&self, index: usize) -> Option<&str> {
        self.column_types.get(index).map(|s| s.as_str())
    }
}

/// JNI ResultSet creation
#[no_mangle]
pub extern "system" fn Java_com_octopus_OctopusResultSet_create(
    _env: JNIEnv,
    _class: JClass,
    result_handle: jlong,
) -> jlong {
    // Result handle would contain serialized QueryResult
    // For simplicity, return 0 (actual implementation would deserialize)
    if result_handle == 0 {
        return 0;
    }
    0
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::Map;

    fn create_test_result() -> QueryResult {
        let mut row1 = Map::new();
        row1.insert("id".to_string(), serde_json::Value::Number(1.into()));
        row1.insert("name".to_string(), serde_json::Value::String("test".to_string()));

        let mut row2 = Map::new();
        row2.insert("id".to_string(), serde_json::Value::Number(2.into()));
        row2.insert("name".to_string(), serde_json::Value::String("prod".to_string()));

        QueryResult {
            query_id: "test-123".to_string(),
            rows: vec![row1, row2],
            schema: vec!["id".to_string(), "name".to_string()],
        }
    }

    #[test]
    fn test_result_set_iteration() {
        let result = create_test_result();
        let mut rs = OctopusResultSet::new(result);

        assert!(rs.next());
        assert_eq!(rs.get_int(1), Some(1));
        assert_eq!(rs.get_string(2), Some("test".to_string()));

        assert!(rs.next());
        assert_eq!(rs.get_int(1), Some(2));
        assert_eq!(rs.get_string(2), Some("prod".to_string()));

        assert!(!rs.next());
    }

    #[test]
    fn test_column_access_by_name() {
        let result = create_test_result();
        let rs = OctopusResultSet::new(result);

        // Initial position before first next()
        assert_eq!(rs.get_string_by_name("name"), None);
    }
}