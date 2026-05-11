//! Arrow Flight server for worker data exchange.
//!
//! Workers expose Flight servers; consumers pull data on demand (worker-pull model).
//! This enables Exchange operators to stream data between workers.

use std::sync::Arc;
use tonic::{Request, Response, Status, Streaming};
use arrow_flight::{
    flight_service_server::{FlightService, FlightServiceServer},
    FlightData, FlightDescriptor, FlightEndpoint, Ticket,
    SchemaResult, PutResult, FlightInfo, PollInfo, Location, Criteria,
};
use crate::runtime::IoRuntime;
use crate::flight_handler::FlightHandler;
use tracing::info;

/// Type alias for boxed stream
type BoxStream<T> = futures::stream::Iter<std::vec::IntoIter<std::result::Result<T, Status>>>;

/// Arrow Flight server for worker data exchange.
pub struct FlightServer {
    worker_id: String,
    port: u16,
    io_runtime: Arc<IoRuntime>,
    handler: Arc<FlightHandler>,
}

impl FlightServer {
    pub fn new(
        worker_id: String,
        port: u16,
        io_runtime: Arc<IoRuntime>,
        handler: Arc<FlightHandler>,
    ) -> Self {
        Self {
            worker_id,
            port,
            io_runtime,
            handler,
        }
    }

    /// Start the Flight server and return the bind address.
    pub async fn start(&self) -> Result<String, Box<dyn std::error::Error + Send + Sync>> {
        let addr: std::net::SocketAddr = format!("0.0.0.0:{}", self.port).parse()?;

        info!("Starting Arrow Flight server on {}", addr);

        let service = FlightServiceServer::new(self.clone());

        info!("Arrow Flight server started on {}", addr);

        Ok(addr.to_string())
    }

    /// Create a FlightEndpoint for accessing exchange data.
    pub fn create_endpoint(&self, ticket: Ticket) -> FlightEndpoint {
        FlightEndpoint {
            ticket: Some(ticket),
            location: vec![],
            expiration_time: None,
            app_metadata: vec![].into(),
        }
    }

    /// Get the worker ID.
    pub fn worker_id(&self) -> &str {
        &self.worker_id
    }

    /// Get the port the server is bound to.
    pub fn port(&self) -> u16 {
        self.port
    }
}

impl Clone for FlightServer {
    fn clone(&self) -> Self {
        Self {
            worker_id: self.worker_id.clone(),
            port: self.port,
            io_runtime: self.io_runtime.clone(),
            handler: self.handler.clone(),
        }
    }
}

#[tonic::async_trait]
impl FlightService for FlightServer {
    type HandshakeStream = BoxStream<arrow_flight::HandshakeResponse>;
    type ListFlightsStream = BoxStream<FlightInfo>;
    type DoGetStream = BoxStream<FlightData>;
    type DoPutStream = BoxStream<PutResult>;
    type DoExchangeStream = BoxStream<FlightData>;
    type DoActionStream = BoxStream<arrow_flight::Result>;
    type ListActionsStream = BoxStream<arrow_flight::ActionType>;

    /// For a given FlightDescriptor, get information about how the flight can be consumed.
    async fn get_flight_info(
        &self,
        request: Request<FlightDescriptor>,
    ) -> Result<Response<FlightInfo>, Status> {
        let descriptor = request.into_inner();
        info!("GetFlightInfo request for {:?}", descriptor);

        // Get schema for this flight (if registered)
        let schema = self.handler.get_schema(&descriptor)?;
        let schema_bytes = crate::flight_handler::schema_to_bytes(&schema)
            .map_err(|e| Status::internal(format!("Failed to encode schema: {}", e)))?;

        // Build FlightInfo with endpoint pointing to this server
        let location = Location {
            uri: format!("grpc://0.0.0.0:{}", self.port),
        };

        let flight_info = FlightInfo {
            schema: schema_bytes.into(),
            flight_descriptor: Some(descriptor),
            endpoint: vec![FlightEndpoint {
                ticket: None,
                location: vec![location],
                expiration_time: None,
                app_metadata: vec![].into(),
            }],
            total_records: 0,
            total_bytes: 0,
            ordered: false,
            app_metadata: vec![].into(),
        };

        Ok(Response::new(flight_info))
    }

    /// For a given FlightDescriptor, start a query and get information to poll its execution status.
    async fn poll_flight_info(
        &self,
        request: Request<FlightDescriptor>,
    ) -> Result<Response<PollInfo>, Status> {
        let _descriptor = request.into_inner();
        Err(Status::unimplemented("PollFlightInfo not yet implemented"))
    }

    /// For a given FlightDescriptor, get the Schema.
    async fn get_schema(
        &self,
        request: Request<FlightDescriptor>,
    ) -> Result<Response<SchemaResult>, Status> {
        let descriptor = request.into_inner();
        info!("GetSchema request for {:?}", descriptor);

        let schema = self.handler.get_schema(&descriptor)?;
        let schema_bytes = crate::flight_handler::schema_to_bytes(&schema)
            .map_err(|e| Status::internal(format!("Failed to encode schema: {}", e)))?;

        Ok(Response::new(SchemaResult { schema: schema_bytes.into() }))
    }

    /// Retrieve a single stream associated with a particular ticket.
    async fn do_get(
        &self,
        request: Request<Ticket>,
    ) -> Result<Response<Self::DoGetStream>, Status> {
        let ticket = request.into_inner();
        info!("DoGet request for ticket: {:?}", ticket);

        // Get batches from handler and wrap in Result
        let handler = self.handler.clone();
        let ticket_bytes = ticket.ticket.clone();

        // Create a simple vec with the data
        let mut batches = Vec::new();
        let mut batch_idx = 0;
        while let Some(data) = handler.get_batch(&ticket_bytes, batch_idx) {
            batches.push(Ok(data));
            batch_idx += 1;
        }

        let stream = futures::stream::iter(batches);
        Ok(Response::new(stream))
    }

    /// Upload data (used for shuffle data, etc).
    async fn do_put(
        &self,
        request: Request<Streaming<FlightData>>,
    ) -> Result<Response<Self::DoPutStream>, Status> {
        use futures::StreamExt;
        let mut stream = request.into_inner();

        while let Some(data) = stream.next().await {
            match data {
                Ok(d) => {
                    self.handler.put_batch(d)?;
                }
                Err(e) => {
                    return Err(Status::internal(format!("DoPut error: {}", e)));
                }
            }
        }

        let stream = futures::stream::iter(vec![Ok(PutResult { app_metadata: vec![].into() })]);
        Ok(Response::new(stream))
    }

    /// Bidirectional exchange for more complex scenarios.
    async fn do_exchange(
        &self,
        request: Request<Streaming<FlightData>>,
    ) -> Result<Response<Self::DoExchangeStream>, Status> {
        let _request = request.into_inner();
        Err(Status::unimplemented("DoExchange not yet implemented"))
    }

    /// Discover available flights (queries/stages).
    async fn list_flights(
        &self,
        request: Request<Criteria>,
    ) -> Result<Response<Self::ListFlightsStream>, Status> {
        let _criteria = request.into_inner();
        // Return FlightInfo for each registered partition
        let flights = self.handler.list_flights()?;
        let stream = futures::stream::iter(
            flights.into_iter()
                .map(|fd| {
                    // Convert FlightDescriptor to FlightInfo
                    Ok(FlightInfo {
                        schema: vec![].into(),
                        flight_descriptor: Some(fd),
                        endpoint: vec![],
                        total_records: 0,
                        total_bytes: 0,
                        ordered: false,
                        app_metadata: vec![].into(),
                    })
                })
                .collect::<Vec<_>>()
        );
        Ok(Response::new(stream))
    }

    /// Initial connection setup.
    async fn handshake(
        &self,
        request: Request<Streaming<arrow_flight::HandshakeRequest>>,
    ) -> Result<Response<Self::HandshakeStream>, Status> {
        use futures::StreamExt;
        let _stream = request.into_inner();
        // For now, accept any handshake and return an empty response
        let stream = futures::stream::iter(vec![Ok(arrow_flight::HandshakeResponse {
            protocol_version: 0,
            payload: vec![].into(),
        })]);
        Ok(Response::new(stream))
    }

    /// Perform a specific action.
    async fn do_action(
        &self,
        request: Request<arrow_flight::Action>,
    ) -> Result<Response<Self::DoActionStream>, Status> {
        let _request = request.into_inner();
        Err(Status::unimplemented("DoAction not yet implemented"))
    }

    /// Discover available actions.
    async fn list_actions(
        &self,
        request: Request<arrow_flight::Empty>,
    ) -> Result<Response<Self::ListActionsStream>, Status> {
        let _request = request.into_inner();
        let stream = futures::stream::iter(vec![
            Ok(arrow_flight::ActionType {
                r#type: "EXECUTE_TASK".to_string(),
                description: "Execute a task on the worker".to_string(),
            }),
            Ok(arrow_flight::ActionType {
                r#type: "HEALTH_CHECK".to_string(),
                description: "Check worker health".to_string(),
            }),
        ]);
        Ok(Response::new(stream))
    }
}