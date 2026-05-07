use tracing_subscriber::FmtSubscriber;
use tracing_subscriber::EnvFilter;
use std::sync::Once;

pub static INIT: Once = Once::new();

#[derive(Debug, Clone, Copy)]
pub enum LogFormat {
    Pretty,
    Structured,
}

impl Default for LogFormat {
    fn default() -> Self {
        LogFormat::Pretty
    }
}

pub fn init_tracing(format: LogFormat) {
    INIT.call_once(|| {
        let filter = EnvFilter::try_from_default_env()
            .unwrap_or_else(|_| EnvFilter::new("info"));

        let subscriber = FmtSubscriber::builder()
            .with_env_filter(filter)
            .with_target(true)
            .with_thread_ids(true)
            .with_file(true)
            .with_line_number(true);

        match format {
            LogFormat::Pretty => {
                subscriber
                    .with_ansi(true)
                    .init();
            },
            LogFormat::Structured => {
                subscriber
                    .with_ansi(false)
                    .json()
                    .init();
            },
        }
    });
}

#[derive(Debug, Clone)]
pub struct QueryTrace {
    pub query_id: String,
    pub sql: String,
    pub start_time: std::time::Instant,
}

impl QueryTrace {
    pub fn new(sql: &str) -> Self {
        Self {
            query_id: uuid_v4(),
            sql: sql.to_string(),
            start_time: std::time::Instant::now(),
        }
    }

    pub fn elapsed(&self) -> std::time::Duration {
        self.start_time.elapsed()
    }

    pub fn log_start(&self) {
        tracing::info!(
            query_id = %self.query_id,
            sql = %self.sql,
            event = "query_start",
            "Query started"
        );
    }

    pub fn log_complete(&self, row_count: usize) {
        let elapsed_ms = self.elapsed().as_millis();
        tracing::info!(
            query_id = %self.query_id,
            row_count = row_count,
            elapsed_ms = elapsed_ms,
            event = "query_complete",
            "Query completed: {} rows in {}ms",
            row_count, elapsed_ms
        );
    }

    pub fn log_error(&self, error: &str) {
        let elapsed_ms = self.elapsed().as_millis();
        tracing::error!(
            query_id = %self.query_id,
            error = %error,
            elapsed_ms = elapsed_ms,
            event = "query_error",
            "Query failed after {}ms: {}",
            elapsed_ms, error
        );
    }
}

fn uuid_v4() -> String {
    use std::time::{SystemTime, UNIX_EPOCH};

    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_nanos();

    format!("{:x}-{:x}-{:x}-{:x}",
        (now >> 96) & 0xffffffff,
        (now >> 64) & 0xffff,
        (now >> 32) & 0xffff,
        now & 0xffffffff
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_query_trace() {
        init_tracing(LogFormat::Pretty);

        let trace = QueryTrace::new("SELECT * FROM test");
        assert!(!trace.query_id.is_empty());
        assert_eq!(trace.sql, "SELECT * FROM test");

        std::thread::sleep(std::time::Duration::from_micros(100));

        let elapsed = trace.elapsed();
        assert!(elapsed.as_millis() >= 0);
    }
}