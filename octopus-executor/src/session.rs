use datafusion::prelude::*;
use datafusion::execution::runtime_env::{RuntimeConfig, RuntimeEnv};
use std::sync::Arc;
use crate::{Result, OctopusError};
use tracing::info;

pub struct ExecutorSession {
    context: SessionContext,
}

impl ExecutorSession {
    pub fn new() -> Result<Self> {
        let config = SessionConfig::new()
            .with_target_partitions(num_cpus::get())
            .with_information_schema(true);

        let runtime = Arc::new(
            RuntimeEnv::try_new(RuntimeConfig::default())
                .map_err(|e| OctopusError::ExecutionError(e.to_string()))?
        );

        let context = SessionContext::new_with_config_rt(config, runtime);

        info!("ExecutorSession created with {} target partitions", num_cpus::get());

        Ok(Self { context })
    }

    pub fn context(&self) -> &SessionContext {
        &self.context
    }
}

impl Default for ExecutorSession {
    fn default() -> Self {
        Self::new().expect("Failed to create default ExecutorSession")
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_session_creation() {
        let session = ExecutorSession::new();
        assert!(session.is_ok());
    }
}
