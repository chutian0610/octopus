//! Octopus JDBC Type 4 Driver
//!
//! Implements java.sql.Driver interface for connecting BI tools to Octopus coordinator.

use jni::objects::{JClass, JString};
use jni::sys::jlong;
use jni::JNIEnv;

mod connection;
mod statement;
mod result_set;
mod metadata;

pub use connection::{OctopusConnection, QueryResult};
pub use statement::{OctopusStatement, OctopusPreparedStatement, ParameterValue};
pub use result_set::{OctopusResultSet, ResultSetMetaData};
pub use metadata::{OctopusDatabaseMetaData, TableInfo, ColumnInfo};

/// Octopus JDBC Driver implementation
///
/// URL format: jdbc:octopus://host:port
pub struct OctopusDriver {
    http_client: reqwest::Client,
}

impl OctopusDriver {
    pub fn new() -> Self {
        Self {
            http_client: reqwest::Client::builder()
                .timeout(std::time::Duration::from_secs(30))
                .build()
                .unwrap_or_default(),
        }
    }

    /// Parse JDBC URL and extract host/port
    /// URL format: jdbc:octopus://host:port
    fn parse_url(url: &str) -> Option<(String, u16)> {
        let url = url.trim_start_matches("jdbc:octopus://");
        let parts: Vec<&str> = url.split(':').collect();
        if parts.len() == 2 {
            let host = parts[0].to_string();
            let port: u16 = parts[1].parse().ok()?;
            Some((host, port))
        } else {
            None
        }
    }

    /// Submit query to coordinator HTTP server
    async fn submit_query(&self, host: &str, port: u16, sql: &str) -> Result<String, String> {
        let url = format!("http://{}:{}/query/submit", host, port);
        let body = serde_json::json!({ "sql": sql });

        let response = self.http_client
            .post(&url)
            .json(&body)
            .send()
            .await
            .map_err(|e| format!("HTTP request failed: {}", e))?;

        if response.status().is_success() {
            let json: serde_json::Value = response.json().await
                .map_err(|e| format!("Failed to parse response: {}", e))?;
            json.get("query_id")
                .and_then(|v| v.as_str())
                .map(|s| s.to_string())
                .ok_or_else(|| "No query_id in response".to_string())
        } else {
            Err(format!("Query submission failed: {}", response.status()))
        }
    }

    /// Poll query state until completed
    async fn poll_query_state(&self, host: &str, port: u16, query_id: &str) -> Result<serde_json::Value, String> {
        let url = format!("http://{}:{}/query/state/{}", host, port, query_id);

        loop {
            let response = self.http_client
                .get(&url)
                .send()
                .await
                .map_err(|e| format!("HTTP request failed: {}", e))?;

            if !response.status().is_success() {
                return Err(format!("Query state request failed: {}", response.status()));
            }

            let json: serde_json::Value = response.json().await
                .map_err(|e| format!("Failed to parse state response: {}", e))?;

            if let Some(state) = json.get("state").and_then(|v| v.as_str()) {
                match state {
                    "completed" => return Ok(json),
                    "failed" => return Err(json.get("error").and_then(|v| v.as_str()).unwrap_or("Query failed").to_string()),
                    _ => {
                        tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;
                    }
                }
            }
        }
    }
}

/// JNI entry point - called when Java loads the driver
#[no_mangle]
pub extern "system" fn Java_com_octopus_JdbcDriver_getDriver(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    // Return pointer to driver as jlong
    let driver = Box::new(OctopusDriver::new());
    Box::into_raw(driver) as jlong
}

/// JNI entry point to connect
#[no_mangle]
pub extern "system" fn Java_com_octopus_JdbcDriver_connect(
    mut _env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jlong {
    let _url_str: String = _env.get_string(&url)
        .map(|s| s.into())
        .unwrap_or_default();

    let driver = Box::new(OctopusDriver::new());
    Box::into_raw(driver) as jlong
}

impl Default for OctopusDriver {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_url_valid() {
        let result = OctopusDriver::parse_url("jdbc:octopus://localhost:50051");
        assert_eq!(result, Some(("localhost".to_string(), 50051)));
    }

    #[test]
    fn test_parse_url_with_ip() {
        let result = OctopusDriver::parse_url("jdbc:octopus://192.168.1.100:8080");
        assert_eq!(result, Some(("192.168.1.100".to_string(), 8080)));
    }
}