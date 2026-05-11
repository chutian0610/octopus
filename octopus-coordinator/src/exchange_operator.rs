//! Exchange operator for distributed data passing between workers.
//!
//! Exchange operators define stage boundaries in distributed query plans.
//! All operators between two Exchanges execute locally on a worker.
//! Data flows via Arrow Flight using worker-pull model.

use std::sync::Arc;
use std::fmt::{self, Debug, Formatter};
use datafusion::physical_plan::{ExecutionPlan, Partitioning, DisplayAs, DisplayFormatType, ExecutionMode};
use datafusion::physical_plan::PlanProperties;
use datafusion::common::Result as ExecResult;
use datafusion::arrow::datatypes::SchemaRef;
use datafusion::physical_expr::EquivalenceProperties;
use datafusion_execution::SendableRecordBatchStream;
use uuid::Uuid;
use serde::{Serialize, Deserialize};

/// Exchange mode determines how data is distributed.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ExchangeMode {
    /// Single partition (e.g., for LIMIT)
    Single,
    /// Hash partition by key
    Hash { keys: Vec<String> },
    /// Round-robin partition
    RoundRobin,
    /// Broadcast to all partitions
    Broadcast,
}

impl ExchangeMode {
    pub fn partition_count(&self) -> usize {
        match self {
            ExchangeMode::Single => 1,
            ExchangeMode::Hash { .. } => 4,  // Default hash partitions
            ExchangeMode::RoundRobin => 4,
            ExchangeMode::Broadcast => 1,
        }
    }

    /// Convert to StagePartitioning for stage planning
    pub fn to_stage_partitioning(&self) -> StagePartitioning {
        match self {
            ExchangeMode::Single => StagePartitioning::Single,
            ExchangeMode::Hash { keys } => StagePartitioning::HashPartitioned {
                num_partitions: self.partition_count(),
                keys: keys.clone(),
            },
            ExchangeMode::RoundRobin => StagePartitioning::RoundRobin {
                num_partitions: self.partition_count(),
            },
            ExchangeMode::Broadcast => StagePartitioning::Broadcast,
        }
    }
}

/// Stage partitioning for planning purposes
#[derive(Debug, Clone)]
pub enum StagePartitioning {
    Single,
    HashPartitioned { num_partitions: usize, keys: Vec<String> },
    RoundRobin { num_partitions: usize },
    Broadcast,
}

/// Exchange operator that defines a stage boundary.
/// Upstream workers produce data; downstream workers consume via Arrow Flight.
pub struct ExchangeOperator {
    /// Unique ID for this exchange
    exchange_id: String,
    /// Input plan (executes on upstream workers)
    input: Arc<dyn ExecutionPlan>,
    /// Exchange mode (how data is partitioned)
    mode: ExchangeMode,
    /// Cached schema
    schema: SchemaRef,
    /// Cached properties
    properties: PlanProperties,
}

impl ExchangeOperator {
    pub fn new(input: Arc<dyn ExecutionPlan>, mode: ExchangeMode) -> Self {
        let schema = input.schema();
        let partitioning = match &mode {
            ExchangeMode::Single => Partitioning::UnknownPartitioning(1),
            ExchangeMode::Hash { .. } => Partitioning::UnknownPartitioning(mode.partition_count()),
            ExchangeMode::RoundRobin => Partitioning::RoundRobinBatch(mode.partition_count()),
            ExchangeMode::Broadcast => Partitioning::UnknownPartitioning(1),
        };
        let eq_properties = EquivalenceProperties::new(schema.clone());
        let properties = PlanProperties::new(
            eq_properties,
            partitioning,
            ExecutionMode::Bounded,
        );
        Self {
            exchange_id: Uuid::new_v4().to_string(),
            input,
            mode,
            schema,
            properties,
        }
    }

    pub fn exchange_id(&self) -> &str {
        &self.exchange_id
    }

    pub fn mode(&self) -> &ExchangeMode {
        &self.mode
    }
}

impl Debug for ExchangeOperator {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        f.debug_struct("ExchangeOperator")
            .field("exchange_id", &self.exchange_id)
            .field("mode", &self.mode)
            .finish()
    }
}

impl DisplayAs for ExchangeOperator {
    fn fmt_as(&self, _: DisplayFormatType, f: &mut Formatter) -> fmt::Result {
        write!(f, "ExchangeOperator: {:?}", self.mode)
    }
}

impl ExecutionPlan for ExchangeOperator {
    fn name(&self) -> &str {
        "ExchangeOperator"
    }

    fn static_name() -> &'static str {
        "ExchangeOperator"
    }

    fn as_any(&self) -> &dyn std::any::Any {
        self
    }

    fn properties(&self) -> &PlanProperties {
        &self.properties
    }

    fn children(&self) -> Vec<&Arc<dyn ExecutionPlan>> {
        vec![&self.input]
    }

    fn with_new_children(
        self: Arc<Self>,
        children: Vec<Arc<dyn ExecutionPlan>>,
    ) -> ExecResult<Arc<dyn ExecutionPlan>> {
        if children.len() != 1 {
            return Err(datafusion::common::DataFusionError::Plan(
                "Exchange requires exactly one child".to_string()
            ));
        }
        Ok(Arc::new(ExchangeOperator::new(
            children[0].clone(),
            self.mode.clone(),
        )))
    }

    fn execute(
        &self,
        partition: usize,
        _context: Arc<datafusion_execution::TaskContext>,
    ) -> ExecResult<SendableRecordBatchStream> {
        // Exchange execution is handled by ExchangeSender/ExchangeReceiver
        // This is a placeholder that returns an error indicating the plan
        // should be executed distributed
        let _ = partition;
        Err(datafusion::common::DataFusionError::Execution(
            "Exchange operator must be executed distributed".to_string()
        ))
    }

    fn benefits_from_input_partitioning(&self) -> Vec<bool> {
        vec![false]
    }
}

/// Metadata for an exchange partition used by workers.
#[derive(Debug, Clone)]
pub struct ExchangePartitionMeta {
    pub exchange_id: String,
    pub partition_index: usize,
    pub total_partitions: usize,
    pub upstream_worker: String,
}

/// Creates ticket string for accessing exchange data from workers.
pub fn create_exchange_ticket(
    exchange_id: &str,
    partition: usize,
) -> String {
    format!("exchange:{}:{}", exchange_id, partition)
}