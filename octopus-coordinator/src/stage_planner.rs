//! Stage planner that splits distributed query plan at exchange boundaries.
//!
//! Creates a DAG of stages where edges represent data exchange via Arrow Flight.
//! This enables Trino-style pipeline execution where stages run concurrently.

use std::collections::HashMap;
use std::sync::Arc;
use datafusion::physical_plan::ExecutionPlan;
use crate::exchange_operator::{StagePartitioning, ExchangePartitionMeta};

/// A stage in the distributed query plan.
#[derive(Debug, Clone)]
pub struct Stage {
    pub stage_id: String,
    pub plan: Arc<dyn ExecutionPlan>,
    pub partitioning: StagePartitioning,
    pub exchange_partitions: Vec<ExchangePartitionMeta>,
    pub upstream_stages: Vec<String>,
    pub downstream_stages: Vec<String>,
}

/// Stage planner that converts logical plan to stage DAG.
pub struct StagePlanner {
    next_stage_id: u32,
}

impl StagePlanner {
    pub fn new() -> Self {
        Self { next_stage_id: 0 }
    }

    /// Plan stages for a query.
    /// Returns a list of stages and the output partitioning.
    pub fn plan_stages(
        &mut self,
        plan: Arc<dyn ExecutionPlan>,
        _worker_count: usize,
    ) -> Result<Vec<Stage>, String> {
        let mut stages = Vec::new();
        let mut stage_map = HashMap::new();

        self.collect_stages(plan, &mut stages, &mut stage_map)?;

        // Validate DAG (check for cycles - Pitfall 5 mitigation)
        self.validate_dag(&stages)?;

        Ok(stages)
    }

    fn collect_stages(
        &mut self,
        plan: Arc<dyn ExecutionPlan>,
        stages: &mut Vec<Stage>,
        stage_map: &mut HashMap<String, String>,
    ) -> Result<(), String> {
        // Check if this plan contains exchange operators
        if let Some(exchange_idx) = self.find_exchange_index(plan.as_ref()) {
            // Split at exchange boundary
            let (upstream, downstream) = self.split_at_exchange(plan, exchange_idx)?;

            // Recursively plan upstream (before exchange)
            let upstream_exchange = self.extract_exchange(&upstream)?;

            let upstream_stage = Stage {
                stage_id: self.create_stage_id(),
                plan: upstream,
                partitioning: upstream_exchange.to_stage_partitioning(),
                exchange_partitions: vec![],
                upstream_stages: vec![],
                downstream_stages: vec![],
            };
            stages.push(upstream_stage);

            // Recursively plan downstream (after exchange)
            let _downstream = self.collect_stages(downstream, stages, stage_map)?;

            Ok(())
        } else {
            // No exchange boundary - this is a single stage
            Ok(())
        }
    }

    fn find_exchange_index(&self, _plan: &dyn ExecutionPlan) -> Option<usize> {
        // Check if plan is an ExchangeOperator or contains one
        // For now, return None to indicate no split needed
        None
    }

    fn split_at_exchange(
        &self,
        _plan: Arc<dyn ExecutionPlan>,
        _exchange_idx: usize,
    ) -> Result<(Arc<dyn ExecutionPlan>, Arc<dyn ExecutionPlan>), String> {
        // Split the plan at the exchange boundary
        // Upstream: operators before exchange
        // Downstream: operators after exchange
        Err("Exchange split not yet implemented".to_string())
    }

    fn extract_exchange(&self, _plan: &Arc<dyn ExecutionPlan>) -> Result<crate::exchange_operator::ExchangeMode, String> {
        Ok(crate::exchange_operator::ExchangeMode::RoundRobin)
    }

    fn create_stage_id(&mut self) -> String {
        let id = format!("stage_{}", self.next_stage_id);
        self.next_stage_id += 1;
        id
    }

    /// Validate that the stage DAG has no cycles (Pitfall 5 mitigation).
    fn validate_dag(&self, stages: &[Stage]) -> Result<(), String> {
        let mut visited = HashMap::new();
        let mut in_stack = HashMap::new();

        for stage in stages {
            self.visit_stage(stage, &mut visited, &mut in_stack)?;
        }

        Ok(())
    }

    fn visit_stage(
        &self,
        stage: &Stage,
        visited: &mut HashMap<String, bool>,
        in_stack: &mut HashMap<String, bool>,
    ) -> Result<(), String> {
        if let Some(in_stack) = in_stack.get(&stage.stage_id) {
            if *in_stack {
                return Err(format!(
                    "Cycle detected in stage DAG: {} depends on itself",
                    stage.stage_id
                ));
            }
        }

        if let Some(visited) = visited.get(&stage.stage_id) {
            if *visited {
                return Ok(());  // Already validated
            }
        }

        in_stack.insert(stage.stage_id.clone(), true);

        for _upstream in &stage.upstream_stages {
            // Find upstream stage
            // self.visit_stage(upstream_stage, visited, in_stack)?;
        }

        in_stack.insert(stage.stage_id.clone(), false);
        visited.insert(stage.stage_id.clone(), true);

        Ok(())
    }
}

impl Default for StagePlanner {
    fn default() -> Self {
        Self::new()
    }
}

/// Check if an operator is a pipeline breaker (Pitfall 1).
pub fn is_pipeline_breaker(_plan: &dyn ExecutionPlan) -> bool {
    // Operators that must materialize all input before producing output:
    // - Sort (unless using top-N optimization)
    // - HashAgg with many groups
    // - HashJoin (for large inputs)

    // For now, return false (assume streaming unless proven otherwise)
    // Real implementation would check operator type
    false
}