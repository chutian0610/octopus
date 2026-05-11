//! Separate CPU and IO runtimes for worker execution.
//!
//! This addresses Pitfall 2: Tokio runtime contention where DataFusion performs
//! both CPU-intensive compute and IO (S3 reads, Arrow Flight) on the same runtime.

use std::sync::Arc;
use tokio::runtime::Builder;

/// CPU-bound runtime using a dedicated thread pool.
/// Compute-intensive query execution runs here to avoid blocking IO tasks.
pub struct CpuRuntime {
    runtime: tokio::runtime::Runtime,
}

impl CpuRuntime {
    /// Create a new CPU runtime with thread pool sized to CPU cores.
    pub fn new() -> Result<Self, std::io::Error> {
        let num_cpus = num_cpus::get();
        let runtime = Builder::new_multi_thread()
            .worker_threads(num_cpus)
            .thread_name("octopus-cpu")
            .enable_all()
            .build()?;

        tracing::info!("CPU runtime initialized with {} threads", num_cpus);
        Ok(Self { runtime })
    }

    /// Spawn a compute task onto the CPU thread pool.
    pub fn spawn<F>(&self, future: F) -> tokio::task::JoinHandle<F::Output>
    where
        F: std::future::Future + Send + 'static,
        F::Output: Send + 'static,
    {
        self.runtime.spawn(future)
    }

    /// Execute a blocking operation on the CPU thread pool.
    pub fn spawn_blocking<F, T>(&self, f: F) -> tokio::task::JoinHandle<T>
    where
        F: FnOnce() -> T + Send + 'static,
        T: Send + 'static,
    {
        self.runtime.spawn_blocking(f)
    }

    /// Get a handle to the CPU runtime for nested spawns.
    pub fn handle(&self) -> tokio::runtime::Handle {
        self.runtime.handle().clone()
    }
}

/// IO-bound runtime using Tokio's default multi-thread runtime.
/// Handles async network IO (Arrow Flight, coordinator communication).
pub struct IoRuntime {
    runtime: tokio::runtime::Runtime,
}

impl IoRuntime {
    /// Create a new IO runtime for network-bound tasks.
    pub fn new() -> Result<Self, std::io::Error> {
        let runtime = Builder::new_multi_thread()
            .worker_threads(num_cpus::get() * 2)  // More threads for IO concurrency
            .thread_name("octopus-io")
            .enable_all()
            .build()?;

        tracing::info!("IO runtime initialized");
        Ok(Self { runtime })
    }

    /// Spawn an IO-bound task onto the IO runtime.
    pub fn spawn<F>(&self, future: F) -> tokio::task::JoinHandle<F::Output>
    where
        F: std::future::Future + Send + 'static,
        F::Output: Send + 'static,
    {
        self.runtime.spawn(future)
    }

    /// Get a handle to the IO runtime for nested spawns.
    pub fn handle(&self) -> tokio::runtime::Handle {
        self.runtime.handle().clone()
    }
}

/// Unified worker runtime combining CPU and IO runtimes.
pub struct WorkerRuntime {
    pub cpu: Arc<CpuRuntime>,
    pub io: Arc<IoRuntime>,
}

impl WorkerRuntime {
    pub fn new() -> Result<Self, std::io::Error> {
        Ok(Self {
            cpu: Arc::new(CpuRuntime::new()?),
            io: Arc::new(IoRuntime::new()?),
        })
    }
}

impl Default for WorkerRuntime {
    fn default() -> Self {
        Self::new().expect("Failed to create worker runtime")
    }
}