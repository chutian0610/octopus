//! Flight handler for managing data streams and exchange partitions.
//!
//! Manages the actual data retrieval for Exchange operators.
//! Handles ticket resolution, schema lookup, and record batch streaming.

use std::collections::HashMap;
use std::sync::{Arc, RwLock};
use arrow_flight::{FlightData, FlightDescriptor, Ticket};
use arrow_schema::Schema;
use arrow_ipc::{writer, reader};
use crate::task_processor::TaskProcessor;
use tracing::info;

/// Handles Flight requests for data exchange.
pub struct FlightHandler {
    /// Active exchange partitions being served
    partitions: RwLock<HashMap<String, ExchangePartition>>,
    /// Task processor for executing queries
    processor: Arc<TaskProcessor>,
}

struct ExchangePartition {
    query_id: String,
    stage: u32,
    partition: u32,
    schema: Schema,
}

impl FlightHandler {
    pub fn new(processor: Arc<TaskProcessor>) -> Self {
        Self {
            partitions: RwLock::new(HashMap::new()),
            processor,
        }
    }

    /// Get schema for a flight descriptor.
    pub fn get_schema(&self, descriptor: &FlightDescriptor) -> Result<Schema, tonic::Status> {
        let key = Self::descriptor_key(descriptor);

        let partitions = self.partitions.read().map_err(|e| {
            tonic::Status::internal(format!("Lock error: {}", e))
        })?;

        partitions.get(&key)
            .map(|p| p.schema.clone())
            .ok_or_else(|| tonic::Status::not_found(format!("Partition not found: {}", key)))
    }

    /// Get a record batch for a ticket.
    pub fn get_batch(&self, ticket: &[u8], _batch_idx: usize) -> Option<FlightData> {
        let key = String::from_utf8_lossy(ticket).to_string();

        let partitions = self.partitions.read().ok()?;

        let _partition = partitions.get(&key)?;

        // In real implementation:
        // - Fetch the actual data from the exchange channel
        // - Return as FlightData with schema and batch

        // Placeholder: Return empty batch
        None
    }

    /// Register a new exchange partition.
    pub fn register_partition(
        &self,
        query_id: String,
        stage: u32,
        partition: u32,
        schema: Schema,
    ) -> Result<String, tonic::Status> {
        let key = format!("{}:{}-{}", query_id, stage, partition);

        let partition = ExchangePartition {
            query_id,
            stage,
            partition,
            schema,
        };

        let mut partitions = self.partitions.write().map_err(|e| {
            tonic::Status::internal(format!("Lock error: {}", e))
        })?;

        partitions.insert(key.clone(), partition);

        info!("Registered exchange partition: {}", key);

        Ok(key)
    }

    /// List available flights (queries/stages).
    pub fn list_flights(&self) -> Result<Vec<FlightDescriptor>, tonic::Status> {
        let partitions = self.partitions.read().map_err(|e| {
            tonic::Status::internal(format!("Lock error: {}", e))
        })?;

        let mut descriptors = Vec::new();

        for (key, _partition) in partitions.iter() {
            descriptors.push(FlightDescriptor {
                r#type: arrow_flight::flight_descriptor::DescriptorType::Cmd as i32,
                cmd: key.as_bytes().to_vec().into(),
                path: vec![],
            });
        }

        Ok(descriptors)
    }

    /// Handle batch upload (DoPut).
    pub fn put_batch(&self, data: FlightData) -> Result<(), tonic::Status> {
        info!("Received batch: {} bytes", data.data_body.len());
        Ok(())
    }

    /// Extract key from flight descriptor.
    fn descriptor_key(descriptor: &FlightDescriptor) -> String {
        if !descriptor.cmd.is_empty() {
            String::from_utf8_lossy(&descriptor.cmd).to_string()
        } else if !descriptor.path.is_empty() {
            descriptor.path.join("/")
        } else {
            String::new()
        }
    }
}

/// Convert Schema to IPC bytes for Flight protocol.
/// The schema is serialized in Arrow IPC format as required by FlightInfo.schema.
pub fn schema_to_bytes(schema: &Schema) -> Result<Vec<u8>, Box<dyn std::error::Error + Send + Sync>> {
    use std::io::Cursor;

    let mut buffer = Vec::new();
    {
        let mut writer = writer::FileWriter::try_new_buffered(Cursor::new(&mut buffer), schema)?;
        writer.finish()?;
    }
    Ok(buffer)
}