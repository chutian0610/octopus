//! Octopus JDBC Connection implementation

use jni::objects::{JClass, JString};
use jni::sys::jlong;
use jni::JNIEnv;
use std::sync::Arc;
use reqwest::Client;
use thiserror::Error;

/// Connection errors
#[derive(Error, Debug)]
pub enum ConnectionError {
    #[error("HTTP request failed: {0}")]
    HttpError(String),
    #[error("Invalid response: {0}")]
    InvalidResponse(String),
    #[error("Query failed: {0}")]
    QueryFailed(String),
    #[error("Connection closed")]
    ConnectionClosed,
}

/// Octopus JDBC Connection
///
/// Represents a connection to the Octopus coordinator via HTTP.
pub struct OctopusConnection {
    host: String,
    port: u16,
    http_client: Client,
    closed: bool,
    active_statements: usize,
}

impl OctopusConnection {
    pub fn new(host: String, port: u16) -> Self {
        Self {
            host,
            port,
            http_client: Client::builder()
                .timeout(std::time::Duration::from_secs(30))
                .build()
                .unwrap_or_default(),
            closed: false,
            active_statements: 0,
        }
    }

    /// Submit SQL query to coordinator
    pub async fn execute_query(&self, sql: &str) -> Result<QueryResult, ConnectionError> {
        if self.closed {
            return Err(ConnectionError::ConnectionClosed);
        }

        // Submit query
        let url = format!("http://{}:{}/query/submit", self.host, self.port);
        let body = serde_json::json!({ "sql": sql });

        let response = self.http_client
            .post(&url)
            .json(&body)
            .send()
            .await
            .map_err(|e| ConnectionError::HttpError(e.to_string()))?;

        if !response.status().is_success() {
            return Err(ConnectionError::InvalidResponse(format!(
                "Query submission failed: {}",
                response.status()
            )));
        }

        let json: serde_json::Value = response.json().await
            .map_err(|e| ConnectionError::InvalidResponse(format!("Failed to parse response: {}", e)))?;

        let query_id = json.get("query_id")
            .and_then(|v| v.as_str())
            .ok_or_else(|| ConnectionError::InvalidResponse("No query_id in response".to_string()))?
            .to_string();

        // Poll for completion
        self.poll_until_completed(&query_id).await
    }

    /// Poll query state until completed or failed
    async fn poll_until_completed(&self, query_id: &str) -> Result<QueryResult, ConnectionError> {
        let url = format!("http://{}:{}/query/state/{}", self.host, self.port, query_id);

        loop {
            let response = self.http_client
                .get(&url)
                .send()
                .await
                .map_err(|e| ConnectionError::HttpError(e.to_string()))?;

            if !response.status().is_success() {
                return Err(ConnectionError::InvalidResponse(format!(
                    "Query state request failed: {}",
                    response.status()
                )));
            }

            let json: serde_json::Value = response.json().await
                .map_err(|e| ConnectionError::InvalidResponse(format!("Failed to parse state: {}", e)))?;

            if let Some(state) = json.get("state").and_then(|v| v.as_str()) {
                match state {
                    "completed" => {
                        let rows = json.get("rows")
                            .and_then(|v| v.as_array())
                            .map(|arr| {
                                arr.iter().filter_map(|row| {
                                    if let serde_json::Value::Object(m) = row {
                                        Some(m.clone())
                                    } else {
                                        None
                                    }
                                }).collect()
                            })
                            .unwrap_or_default();

                        let schema = json.get("schema")
                            .and_then(|v| v.as_array())
                            .map(|arr| {
                                arr.iter().filter_map(|s| s.as_str().map(|t| t.to_string())).collect()
                            })
                            .unwrap_or_default();

                        return Ok(QueryResult { query_id: query_id.to_string(), rows, schema });
                    }
                    "failed" => {
                        let error = json.get("error")
                            .and_then(|v| v.as_str())
                            .unwrap_or("Query failed")
                            .to_string();
                        return Err(ConnectionError::QueryFailed(error));
                    }
                    _ => {
                        // Still running, wait and poll again
                        tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;
                    }
                }
            }
        }
    }

    pub fn is_closed(&self) -> bool {
        self.closed
    }

    pub fn get_active_statements(&self) -> usize {
        self.active_statements
    }

    pub fn increment_statements(&mut self) {
        self.active_statements += 1;
    }

    pub fn decrement_statements(&mut self) {
        if self.active_statements > 0 {
            self.active_statements -= 1;
        }
    }
}

/// Query result containing rows and schema
pub struct QueryResult {
    pub query_id: String,
    pub rows: Vec<serde_json::Map<String, serde_json::Value>>,
    pub schema: Vec<String>,
}

/// JNI connect method
#[no_mangle]
pub extern "system" fn Java_com_octopus_OctopusConnection_create(
    mut _env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jlong {
    let url_str: String = _env.get_string(&url)
        .map(|s| s.into())
        .unwrap_or_default();

    // Parse URL: jdbc:octopus://host:port
    let url_part = url_str.trim_start_matches("jdbc:octopus://");
    let parts: Vec<&str> = url_part.split(':').collect();

    if parts.len() != 2 {
        return 0; // Return null handle on error
    }

    let host = parts[0].to_string();
    let port: u16 = parts[1].parse().unwrap_or(50051);

    let conn = Box::new(OctopusConnection::new(host, port));
    Box::into_raw(conn) as jlong
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_connection_creation() {
        let conn = OctopusConnection::new("localhost".to_string(), 50051);
        assert!(!conn.is_closed());
        assert_eq!(conn.get_active_statements(), 0);
    }
}