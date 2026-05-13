//! Octopus JDBC DatabaseMetaData implementation

use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jint, jlong, jstring, JNI_TRUE};
use jni::JNIEnv;
use std::sync::Arc;
use crate::connection::OctopusConnection;

/// DatabaseMetaData for BI tool introspection
pub struct OctopusDatabaseMetaData {
    connection: Arc<OctopusConnection>,
    driver_name: String,
    driver_version: String,
    jdbc_major_version: i32,
    jdbc_minor_version: i32,
}

impl OctopusDatabaseMetaData {
    pub fn new(connection: Arc<OctopusConnection>) -> Self {
        Self {
            connection,
            driver_name: "Octopus JDBC Driver".to_string(),
            driver_version: "0.1.0".to_string(),
            jdbc_major_version: 4,
            jdbc_minor_version: 2,
        }
    }

    /// Get all tables (placeholder - would query coordinator for schema)
    pub fn get_tables(&self) -> Vec<TableInfo> {
        // In a full implementation, this would query the coordinator's
        // information_schema or similar to get actual table metadata
        vec![
            TableInfo {
                catalog: None,
                schema: Some("public".to_string()),
                name: "users".to_string(),
                table_type: "TABLE".to_string(),
            },
            TableInfo {
                catalog: None,
                schema: Some("public".to_string()),
                name: "orders".to_string(),
                table_type: "TABLE".to_string(),
            },
        ]
    }

    /// Get columns for a table
    pub fn get_columns(&self, table_name: &str) -> Vec<ColumnInfo> {
        // Placeholder - would query coordinator for actual column metadata
        match table_name {
            "users" => vec![
                ColumnInfo {
                    table_cat: None,
                    table_schem: Some("public".to_string()),
                    table_name: "users".to_string(),
                    column_name: "id".to_string(),
                    data_type: 4, // INTEGER
                    type_name: "INTEGER".to_string(),
                    column_size: Some(10),
                    decimal_digits: None,
                    num_prec_radix: 10,
                    nullable: Some(0),
                    remarks: None,
                    column_def: None,
                    char_octet_length: None,
                    ordinal_position: 1,
                    is_nullable: "NO".to_string(),
                },
                ColumnInfo {
                    table_cat: None,
                    table_schem: Some("public".to_string()),
                    table_name: "users".to_string(),
                    column_name: "name".to_string(),
                    data_type: 12, // VARCHAR
                    type_name: "VARCHAR".to_string(),
                    column_size: Some(255),
                    decimal_digits: None,
                    num_prec_radix: 10,
                    nullable: Some(0),
                    remarks: None,
                    column_def: None,
                    char_octet_length: None,
                    ordinal_position: 2,
                    is_nullable: "NO".to_string(),
                },
            ],
            _ => vec![],
        }
    }

    pub fn get_driver_name(&self) -> &str {
        &self.driver_name
    }

    pub fn get_driver_version(&self) -> &str {
        &self.driver_version
    }

    pub fn get_jdbc_major_version(&self) -> i32 {
        self.jdbc_major_version
    }

    pub fn get_jdbc_minor_version(&self) -> i32 {
        self.jdbc_minor_version
    }

    pub fn accepts_url(&self, url: &str) -> bool {
        url.starts_with("jdbc:octopus://")
    }

    pub fn get_max_row_size(&self) -> i32 {
        65535
    }

    pub fn get_max_connections(&self) -> i32 {
        100
    }

    pub fn get_max_statement_length(&self) -> i32 {
        65536
    }
}

/// Table information
pub struct TableInfo {
    pub catalog: Option<String>,
    pub schema: Option<String>,
    pub name: String,
    pub table_type: String,
}

/// Column information
pub struct ColumnInfo {
    pub table_cat: Option<String>,
    pub table_schem: Option<String>,
    pub table_name: String,
    pub column_name: String,
    pub data_type: i32,
    pub type_name: String,
    pub column_size: Option<i32>,
    pub decimal_digits: Option<i32>,
    pub num_prec_radix: i32,
    pub nullable: Option<i32>,
    pub remarks: Option<String>,
    pub column_def: Option<String>,
    pub char_octet_length: Option<i32>,
    pub ordinal_position: i32,
    pub is_nullable: String,
}

/// JNI DatabaseMetaData creation
#[no_mangle]
pub extern "system" fn Java_com_octopus_OctopusDatabaseMetaData_create(
    _env: JNIEnv,
    _class: JClass,
    connection_handle: jlong,
) -> jlong {
    if connection_handle == 0 {
        return 0;
    }

    let conn = unsafe { Arc::from_raw(connection_handle as *const OctopusConnection) };
    let metadata = Box::new(OctopusDatabaseMetaData::new(conn.clone()));
    std::mem::forget(conn);
    Box::into_raw(metadata) as jlong
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_metadata_creation() {
        let conn = Arc::new(OctopusConnection::new("localhost".to_string(), 50051));
        let meta = OctopusDatabaseMetaData::new(conn);

        assert_eq!(meta.get_driver_name(), "Octopus JDBC Driver");
        assert_eq!(meta.get_jdbc_major_version(), 4);
        assert!(meta.accepts_url("jdbc:octopus://localhost:50051"));
        assert!(!meta.accepts_url("jdbc:postgresql://localhost:5432"));
    }

    #[test]
    fn test_get_tables() {
        let conn = Arc::new(OctopusConnection::new("localhost".to_string(), 50051));
        let meta = OctopusDatabaseMetaData::new(conn);
        let tables = meta.get_tables();

        assert!(!tables.is_empty());
    }
}