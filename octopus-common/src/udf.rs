//! UDF/UDTF Registry for Octopus
//!
//! This module provides UDF (User-Defined Function) registration and management.
//! Registered functions are available in SQL queries via DataFusion's SessionContext.
//!
//! # Example
//!
//! ```ignore
//! use octopus_common::udf::{UdfRegistry, UdfRegistryImpl, create_simple_udf};
//!
//! let registry = UdfRegistryImpl::new();
//! let to_upper = create_simple_udf(
//!     "to_upper",
//!     vec![DataType::Utf8],
//!     DataType::Utf8,
//!     Volatility::Stable,
//!     |args: &[ColumnarValue]| {
//!         // implementation here
//!     },
//! );
//! registry.register_scalar("to_upper", to_upper).await;
//! ```

use async_trait::async_trait;
use datafusion::arrow::datatypes::DataType;
use datafusion::physical_plan::ColumnarValue;
use datafusion_expr::{create_udf, ScalarUDF, Volatility};
use std::sync::Arc;

/// Re-export DataFusion types for UDF/UDTF creation
pub use datafusion::arrow::datatypes::DataType as ArrowDataType;

/// Type alias for the implementation function used in UDF creation
pub type UdfFunction = Arc<dyn Fn(&[ColumnarValue]) -> datafusion::common::Result<ColumnarValue> + Send + Sync>;

/// Result type for UDF operations
pub type UdfResult<T> = std::result::Result<T, Box<dyn std::error::Error + Send + Sync>>;

/// Unified registry for both scalar UDFs and table UDTFs
#[async_trait]
pub trait UdfRegistry: Send + Sync {
    /// Register a scalar (single-value) UDF
    async fn register_scalar(&self, name: &str, func: ScalarUDF) -> UdfResult<()>;

    /// Get a registered scalar UDF by name
    fn get_scalar(&self, name: &str) -> Option<ScalarUDF>;

    /// List all registered function names (both scalar and table)
    fn list_functions(&self) -> Vec<(String, String)>;
}

/// In-memory implementation of UdfRegistry using HashMaps with interior mutability
#[derive(Debug, Default)]
pub struct UdfRegistryImpl {
    scalar_functions: std::sync::RwLock<std::collections::HashMap<String, ScalarUDF>>,
}

impl UdfRegistryImpl {
    /// Create a new empty UDF registry
    pub fn new() -> Self {
        Self {
            scalar_functions: std::sync::RwLock::new(std::collections::HashMap::new()),
        }
    }

    /// Get the number of registered scalar functions
    pub fn scalar_count(&self) -> usize {
        self.scalar_functions.read().unwrap().len()
    }
}

#[async_trait]
impl UdfRegistry for UdfRegistryImpl {
    async fn register_scalar(&self, name: &str, func: ScalarUDF) -> UdfResult<()> {
        let key = name.to_lowercase();
        self.scalar_functions.write().unwrap().insert(key, func);
        Ok(())
    }

    fn get_scalar(&self, name: &str) -> Option<ScalarUDF> {
        self.scalar_functions.read().unwrap().get(&name.to_lowercase()).cloned()
    }

    fn list_functions(&self) -> Vec<(String, String)> {
        let guard = self.scalar_functions.read().unwrap();
        let mut result = Vec::with_capacity(guard.len());
        for (name, func) in guard.iter() {
            result.push((name.clone(), format!("scalar: {:?}", func.name())));
        }
        result
    }
}

/// Create a simple scalar UDF from a function closure.
///
/// This is a convenience function that wraps DataFusion's `create_udf`.
///
/// # Arguments
///
/// * `name` - The SQL-callable name of the function
/// * `input_types` - The expected argument types
/// * `return_type` - The return type of the function
/// * `volatility` - The volatility of the function (Stable, Immutable, Volatile)
/// * `fun` - The function implementation
///
/// # Example
///
/// ```ignore
/// let to_upper = create_simple_udf(
///     "to_upper",
///     vec![DataType::Utf8],
///     DataType::Utf8,
///     Volatility::Stable,
///     Arc::new(|args: &[ColumnarValue]| {
///         // implementation here
///     }),
/// );
/// ```
pub fn create_simple_udf(
    name: &str,
    input_types: Vec<DataType>,
    return_type: DataType,
    volatility: Volatility,
    fun: UdfFunction,
) -> ScalarUDF {
    create_udf(name, input_types, return_type, volatility, fun)
}

#[cfg(test)]
mod tests {
    use super::*;
    use datafusion::arrow::array::{StringArray, AsArray};
    use datafusion::scalar::ScalarValue;

    #[tokio::test]
    async fn test_registry_operations() {
        let registry = UdfRegistryImpl::new();

        // Initially empty
        assert_eq!(registry.scalar_count(), 0);
        assert!(registry.list_functions().is_empty());

        // to_upper example UDF - handles only Array input
        let to_upper = create_simple_udf(
            "to_upper",
            vec![DataType::Utf8],
            DataType::Utf8,
            Volatility::Stable,
            Arc::new(|args: &[ColumnarValue]| {
                match &args[0] {
                    ColumnarValue::Array(arr) => {
                        let s_arr = arr.as_string_view();
                        let result: StringArray = s_arr
                            .iter()
                            .map(|s| s.map(|s| s.to_uppercase()))
                            .collect();
                        Ok(ColumnarValue::Array(Arc::new(result)))
                    }
                    ColumnarValue::Scalar(_) => {
                        // Return null for scalar input (simplified for testing)
                        Ok(ColumnarValue::Scalar(ScalarValue::Utf8(None)))
                    }
                }
            }),
        );

        registry.register_scalar("to_upper", to_upper).await.unwrap();

        assert_eq!(registry.scalar_count(), 1);
        assert!(registry.get_scalar("to_upper").is_some());
        assert!(registry.get_scalar("TO_UPPER").is_some()); // case insensitive
        assert!(registry.get_scalar("unknown").is_none());

        let funcs = registry.list_functions();
        assert_eq!(funcs.len(), 1);
        assert_eq!(funcs[0].0, "to_upper");
    }
}