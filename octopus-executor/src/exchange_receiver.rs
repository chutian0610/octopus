//! Exchange receiver that pulls data from upstream workers via Arrow Flight.
//!
//! Implements worker-pull model: downstream workers pull data from upstream.
//! This provides natural backpressure as consumers request data at their pace.

use futures::{Stream, StreamExt};
use tonic::transport::Channel;
use bytes::Bytes;
use arrow_flight::{
    flight_service_client::FlightServiceClient,
    FlightData, Ticket,
};
use arrow::record_batch::RecordBatch;
use arrow::ipc::reader::StreamReader;
use std::io::Cursor;
use tracing::info;

/// Receiver for consuming exchange data from upstream workers.
pub struct ExchangeReceiver {
    /// Upstream worker addresses
    upstream_workers: Vec<String>,
    /// Partition index this receiver handles
    partition_index: usize,
    /// Flight clients for each upstream worker
    flight_clients: Vec<FlightServiceClient<Channel>>,
}

impl ExchangeReceiver {
    /// Create a new ExchangeReceiver by connecting to upstream workers.
    pub async fn new(
        upstream_workers: Vec<String>,
        partition_index: usize,
    ) -> Result<Self, String> {
        let mut flight_clients = Vec::new();

        for worker_addr in &upstream_workers {
            let channel = Channel::from_shared(worker_addr.clone())
                .map_err(|e| format!("Failed to create channel: {}", e))?
                .connect()
                .await
                .map_err(|e| format!("Failed to connect to {}: {}", worker_addr, e))?;

            flight_clients.push(FlightServiceClient::new(channel));
        }

        info!("ExchangeReceiver created for partition {} with {} upstream workers",
              partition_index, upstream_workers.len());

        Ok(Self {
            upstream_workers,
            partition_index,
            flight_clients,
        })
    }

    /// Create a stream for pulling data from upstream.
    pub fn create_stream(
        &self,
        exchange_id: &str,
    ) -> impl Stream<Item = Result<RecordBatch, datafusion::common::DataFusionError>> + Send {
        let ticket = Ticket {
            ticket: Bytes::from(format!("exchange:{}:{}", exchange_id, self.partition_index)),
        };

        let client_idx = self.partition_index % self.flight_clients.len();
        let mut client = self.flight_clients[client_idx].clone();

        async_stream::stream! {
            let request = tonic::Request::new(ticket);

            match client.do_get(request).await {
                Ok(response) => {
                    let mut stream = response.into_inner();
                    while let Some(result) = stream.next().await {
                        match result {
                            Ok(flight_data) => {
                                match Self::deserialize_batch(flight_data) {
                                    Ok(batch) => yield Ok(batch),
                                    Err(e) => yield Err(e),
                                }
                            }
                            Err(e) => {
                                yield Err(datafusion::common::DataFusionError::Execution(
                                    format!("Flight stream error: {}", e)
                                ));
                                break;
                            }
                        }
                    }
                }
                Err(e) => {
                    yield Err(datafusion::common::DataFusionError::Execution(
                        format!("Failed to get flight data: {}", e)
                    ));
                }
            }
        }
    }

    /// Deserialize FlightData to RecordBatch.
    fn deserialize_batch(flight_data: FlightData) -> Result<RecordBatch, datafusion::common::DataFusionError> {
        let cursor = Cursor::new(flight_data.data_body);
        let mut reader = StreamReader::try_new(cursor, None)
            .map_err(|e| datafusion::common::DataFusionError::External(Box::new(e)))?;

        let batch = reader.next()
            .transpose()
            .map_err(|e| datafusion::common::DataFusionError::External(Box::new(e)))?
            .ok_or_else(|| datafusion::common::DataFusionError::Execution("Empty flight data".to_string()))?;
        Ok(batch)
    }
}

impl Drop for ExchangeReceiver {
    fn drop(&mut self) {
        info!("ExchangeReceiver dropped for partition {}", self.partition_index);
    }
}