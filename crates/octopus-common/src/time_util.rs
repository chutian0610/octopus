use std::time::{SystemTime, UNIX_EPOCH};

pub fn to_millis(time: SystemTime) -> i64 {
    time.duration_since(UNIX_EPOCH).unwrap().as_millis() as i64
}
