//! Octopus JDBC Statement and PreparedStatement implementation

use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jint, jlong, jstring, JNI_TRUE};
use jni::JNIEnv;
use std::sync::Arc;
use crate::connection::OctopusConnection;
use crate::result_set::OctopusResultSet;

/// Octopus JDBC Statement
///
/// Executes SQL queries against the Octopus coordinator.
pub struct OctopusStatement {
    connection: Arc<OctopusConnection>,
    query_result: Option<crate::connection::QueryResult>,
    current_row: usize,
    closed: bool,
}

impl OctopusStatement {
    pub fn new(connection: Arc<OctopusConnection>) -> Self {
        Self {
            connection,
            query_result: None,
            current_row: 0,
            closed: false,
        }
    }

    /// Execute a SQL query and return results
    pub async fn execute_query(&mut self, sql: &str) -> Result<(), String> {
        if self.closed {
            return Err("Statement is closed".to_string());
        }

        let result = self.connection.execute_query(sql).await
            .map_err(|e| e.to_string())?;

        self.query_result = Some(result);
        self.current_row = 0;
        Ok(())
    }

    /// Get current query result
    pub fn get_result(&self) -> Option<&crate::connection::QueryResult> {
        self.query_result.as_ref()
    }

    /// Check if there are more rows
    pub fn has_more_rows(&self) -> bool {
        self.query_result
            .as_ref()
            .map(|r| self.current_row < r.rows.len())
            .unwrap_or(false)
    }

    /// Get current row data
    pub fn get_current_row(&self) -> Option<&serde_json::Map<String, serde_json::Value>> {
        self.query_result
            .as_ref()
            .and_then(|r| r.rows.get(self.current_row))
    }

    /// Advance to next row
    pub fn next_row(&mut self) -> bool {
        if self.has_more_rows() {
            self.current_row += 1;
            true
        } else {
            false
        }
    }

    pub fn is_closed(&self) -> bool {
        self.closed
    }

    pub fn close(&mut self) {
        self.closed = true;
        self.query_result = None;
    }
}

/// Octopus JDBC PreparedStatement
///
/// Pre-compiled SQL with parameter binding.
pub struct OctopusPreparedStatement {
    sql: String,
    parameters: Vec<ParameterValue>,
    connection: Arc<OctopusConnection>,
    closed: bool,
}

#[derive(Debug, Clone)]
pub enum ParameterValue {
    Int(i32),
    String(String),
    Double(f64),
    Boolean(bool),
    Null,
}

impl OctopusPreparedStatement {
    pub fn new(sql: String, connection: Arc<OctopusConnection>) -> Self {
        Self {
            sql,
            parameters: Vec::new(),
            connection,
            closed: false,
        }
    }

    /// Set integer parameter
    pub fn set_int(&mut self, param_index: usize, value: i32) {
        self.ensure_params_size(param_index);
        self.parameters[param_index] = ParameterValue::Int(value);
    }

    /// Set string parameter
    pub fn set_string(&mut self, param_index: usize, value: String) {
        self.ensure_params_size(param_index);
        self.parameters[param_index] = ParameterValue::String(value);
    }

    /// Set double parameter
    pub fn set_double(&mut self, param_index: usize, value: f64) {
        self.ensure_params_size(param_index);
        self.parameters[param_index] = ParameterValue::Double(value);
    }

    /// Set boolean parameter
    pub fn set_boolean(&mut self, param_index: usize, value: bool) {
        self.ensure_params_size(param_index);
        self.parameters[param_index] = ParameterValue::Boolean(value);
    }

    /// Set null parameter
    pub fn set_null(&mut self, param_index: usize) {
        self.ensure_params_size(param_index);
        self.parameters[param_index] = ParameterValue::Null;
    }

    fn ensure_params_size(&mut self, index: usize) {
        if index >= self.parameters.len() {
            self.parameters.resize(index + 1, ParameterValue::Null);
        }
    }

    /// Build final SQL with parameters interpolated
    fn build_sql(&self) -> String {
        let mut result = self.sql.clone();
        for param in &self.parameters {
            let replacement = match param {
                ParameterValue::Int(v) => v.to_string(),
                ParameterValue::String(s) => format!("'{}'", s.replace("'", "\\'")),
                ParameterValue::Double(v) => v.to_string(),
                ParameterValue::Boolean(v) => v.to_string(),
                ParameterValue::Null => "NULL".to_string(),
            };
            // Simple replace - for more complex cases would need proper SQL parsing
            if let Some(pos) = result.find('?') {
                result = format!("{}{}{}", &result[..pos], replacement, &result[pos + 1..]);
            }
        }
        result
    }

    /// Execute query with parameters bound
    pub async fn execute_query(&mut self) -> Result<crate::connection::QueryResult, String> {
        if self.closed {
            return Err("PreparedStatement is closed".to_string());
        }

        let sql = self.build_sql();
        self.connection.execute_query(&sql).await
            .map_err(|e| e.to_string())
    }

    pub fn is_closed(&self) -> bool {
        self.closed
    }

    pub fn close(&mut self) {
        self.closed = true;
        self.parameters.clear();
    }
}

/// JNI Statement creation
#[no_mangle]
pub extern "system" fn Java_com_octopus_OctopusStatement_create(
    _env: JNIEnv,
    _class: JClass,
    connection_handle: jlong,
) -> jlong {
    if connection_handle == 0 {
        return 0;
    }

    let conn = unsafe { Arc::from_raw(connection_handle as *const OctopusConnection) };
    let stmt = Box::new(OctopusStatement::new(conn.clone()));
    // Leak the Arc intentionally - caller owns it
    std::mem::forget(conn);
    Box::into_raw(stmt) as jlong
}

/// JNI PreparedStatement creation
#[no_mangle]
pub extern "system" fn Java_com_octopus_OctopusPreparedStatement_create(
    mut _env: JNIEnv,
    _class: JClass,
    sql: JString,
    connection_handle: jlong,
) -> jlong {
    let sql_str: String = _env.get_string(&sql)
        .map(|s| s.into())
        .unwrap_or_default();

    if connection_handle == 0 {
        return 0;
    }

    let conn = unsafe { Arc::from_raw(connection_handle as *const OctopusConnection) };
    let stmt = Box::new(OctopusPreparedStatement::new(sql_str, conn.clone()));
    std::mem::forget(conn);
    Box::into_raw(stmt) as jlong
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parameter_binding() {
        let sql = "SELECT * FROM t WHERE id = ? AND name = ?";
        let mut stmt = OctopusPreparedStatement::new(
            sql.to_string(),
            Arc::new(OctopusConnection::new("localhost".to_string(), 50051)),
        );
        stmt.set_int(0, 42);
        stmt.set_string(1, "test".to_string());

        let built = stmt.build_sql();
        assert!(built.contains("42"));
        assert!(built.contains("'test'"));
    }
}