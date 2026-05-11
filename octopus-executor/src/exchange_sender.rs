//! Exchange sender that pushes data to downstream workers via Arrow Flight.
//!
//! Sends data to downstream workers for the next stage of processing.
//! Works with ExchangeReceiver to form the complete exchange channel.

use arrow::record_batch::RecordBatch;
use arrow::ipc::writer::StreamWriter;
use bytes::Bytes;
use std::io::Cursor;
use tracing::info;

/// Sender for pushing exchange data to downstream workers.
pub struct ExchangeSender {
    /// Downstream worker addresses
    downstream_workers: Vec<String>,
    /// Partition mode for distributing data
    partition_mode: PartitionMode,
}

#[derive(Debug, Clone)]
pub enum PartitionMode {
    Single,
    HashPartition { keys: Vec<String> },
    RoundRobin,
    Broadcast,
}

impl ExchangeSender {
    pub fn new(
        downstream_workers: Vec<String>,
        partition_mode: PartitionMode,
    ) -> Self {
        info!("ExchangeSender created with {} downstream workers",
              downstream_workers.len());

        Self {
            downstream_workers,
            partition_mode,
        }
    }

    /// Send a record batch to downstream workers.
    /// Returns the number of bytes sent.
    pub async fn send(
        &self,
        batch: RecordBatch,
    ) -> Result<usize, String> {
        match &self.partition_mode {
            PartitionMode::Broadcast => {
                self.broadcast_batch(batch).await
            }
            PartitionMode::HashPartition { keys } => {
                self.hash_partition_batch(batch, keys).await
            }
            PartitionMode::RoundRobin => {
                self.round_robin_batch(batch).await
            }
            PartitionMode::Single => {
                self.single_batch(batch).await
            }
        }
    }

    async fn broadcast_batch(&self, batch: RecordBatch) -> Result<usize, String> {
        let mut total_bytes = 0;

        for worker_addr in &self.downstream_workers {
            match self.send_to_worker(worker_addr, batch.clone()).await {
                Ok(bytes) => total_bytes += bytes,
                Err(e) => tracing::warn!("Failed to broadcast to {}: {}", worker_addr, e),
            }
        }

        Ok(total_bytes)
    }

    async fn hash_partition_batch(
        &self,
        batch: RecordBatch,
        _keys: &[String],
    ) -> Result<usize, String> {
        // For hash partitioning, calculate hash of key columns
        // and route to the appropriate worker
        // For now, just send to the first worker (placeholder)
        let worker_addr = self.downstream_workers.first()
            .ok_or_else(|| "No downstream workers".to_string())?;
        self.send_to_worker(worker_addr, batch).await
    }

    async fn round_robin_batch(&self, batch: RecordBatch) -> Result<usize, String> {
        let worker_addr = self.downstream_workers.first()
            .ok_or_else(|| "No downstream workers".to_string())?;
        self.send_to_worker(worker_addr, batch).await
    }

    async fn single_batch(&self, batch: RecordBatch) -> Result<usize, String> {
        let worker_addr = self.downstream_workers.first()
            .ok_or_else(|| "No downstream workers".to_string())?;
        self.send_to_worker(worker_addr, batch).await
    }

    async fn send_to_worker(
        &self,
        _worker_addr: &str,
        batch: RecordBatch,
    ) -> Result<usize, String> {
        // Serialize batch to FlightData
        let flight_data = self.serialize_batch(batch)?;

        // For now, just return the size (actual send would use FlightClient)
        let bytes = flight_data.data_body.len();
        info!("Serialized batch of {} bytes", bytes);

        Ok(bytes)
    }

    /// Serialize RecordBatch to FlightData.
    fn serialize_batch(&self, batch: RecordBatch) -> Result<arrow_flight::FlightData, String> {
        let mut data_body = Vec::new();
        {
            let mut writer = StreamWriter::try_new(
                Cursor::new(&mut data_body),
                &batch.schema(),
            ).map_err(|e| format!("Failed to create writer: {}", e))?;

            writer.write(&batch)
                .map_err(|e| format!("Failed to write batch: {}", e))?;

            writer.finish()
                .map_err(|e| format!("Failed to finish writer: {}", e))?;
        }

        Ok(arrow_flight::FlightData {
            flight_descriptor: None,
            data_body: data_body.into(),
            data_header: Bytes::new(),
            app_metadata: Bytes::new(),
        })
    }
}

impl Drop for ExchangeSender {
    fn drop(&mut self) {
        info!("ExchangeSender dropped");
    }
}