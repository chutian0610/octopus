use chrono::prelude::*;
use chrono_tz::Tz;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use chrono::DateTime;

pub static FORMAT_PATTERN_DATETIME: &str = "%Y-%m-%d %H:%M:%S";
pub static FORMAT_PATTERN_DATETIME_WITH_MICRO: &str = "%Y-%m-%d %H:%M:%S%.3f";
pub fn to_millis(time: SystemTime) -> u64 {
    time.duration_since(UNIX_EPOCH).unwrap().as_millis() as u64
}
pub fn to_micros(time: SystemTime) -> u64 {
    time.duration_since(UNIX_EPOCH).unwrap().as_micros() as u64
}

pub fn to_nanos(time: SystemTime) -> u128 {
    time.duration_since(UNIX_EPOCH).unwrap().as_nanos()
}

pub fn from_millis(ts: u64) -> SystemTime {
    UNIX_EPOCH + Duration::from_millis(ts)
}

pub fn from_micros(ts: u64) -> SystemTime {
    UNIX_EPOCH + Duration::from_micros(ts)
}

pub fn from_nanos(ts: u128) -> SystemTime {
    UNIX_EPOCH
        + Duration::from_secs((ts / 1_000_000_000) as u64)
        + Duration::from_nanos((ts % 1_000_000_000) as u64)
}

pub fn print_local_time(st: SystemTime) -> String {
    let local_datetime: DateTime<Local> = st.into();
    local_datetime.format(FORMAT_PATTERN_DATETIME).to_string()
}

pub fn print_local_time_with_pattern(st: SystemTime, pattern: &str) -> String {
    let local_datetime: DateTime<Local> = st.into();
    local_datetime.format(pattern).to_string()
}

pub fn print_utc_time(st: SystemTime) -> String {
    let utc_datetime: DateTime<Utc> = st.into();
    utc_datetime.format(FORMAT_PATTERN_DATETIME).to_string()
}
pub fn print_utc_time_with_pattern(st: SystemTime, pattern: &str) -> String {
    let utc_datetime: DateTime<Utc> = st.into();
    utc_datetime.format(pattern).to_string()
}

pub fn parse_utc_time(datetime_str: &str) -> Result<SystemTime, String> {
    let utc_datetime = NaiveDateTime::parse_from_str(datetime_str, FORMAT_PATTERN_DATETIME)
        .map(|time: NaiveDateTime| time.and_utc())
        .map_err(|e: chrono::ParseError| format!("parsing date string failed: {}", e))?;
    Ok(utc_datetime.into())
}

pub fn parse_utc_time_with_pattern(
    datetime_str: &str,
    pattern: &str,
) -> Result<SystemTime, String> {
    let utc_datetime = NaiveDateTime::parse_from_str(datetime_str, pattern)
        .map(|time: NaiveDateTime| time.and_utc())
        .map_err(|e: chrono::ParseError| format!("解析日期字符串失败: {}", e))?;
    Ok(utc_datetime.into())
}

pub fn custom_timezone(
    time_str: &str,
    format: &str,
    src_tz: &str,
    target_tz: &str,
) -> Result<DateTime<Tz>, String> {
    let src_timezone = src_tz
        .parse::<Tz>()
        .map_err(|e: chrono_tz::ParseError| format!("解析src_tz失败: {}", e))?;
    let naive = chrono::NaiveDateTime::parse_from_str(time_str, format)
        .map_err(|e: chrono::ParseError| format!("解析日期字符串失败: {}", e))?;
    let src_time = src_timezone.from_local_datetime(&naive).unwrap();
    let target_timezone = target_tz
        .parse::<Tz>()
        .map_err(|e: chrono_tz::ParseError| format!("解析target_tz失败: {}", e))?;
    Ok(src_time.with_timezone(&target_timezone))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_to_millis() {
        let time = parse_utc_time("2025-04-03 07:52:39");
        assert!(time.is_ok());
        assert_eq!(to_millis(time.unwrap()), 1743666759000);
    }
    #[test]
    fn test_from_millis() {
        let time = from_millis(1743666759000);
        assert_eq!(to_millis(time), 1743666759000);
    }
    #[test]
    fn test_format_local_time() {
        let time = from_millis(1743666759000);
        assert_eq!(print_local_time(time), "2025-04-03 15:52:39");
    }
    #[test]
    fn test_format_utc_time() {
        let time = from_millis(1743666759000);
        assert_eq!(print_utc_time(time), "2025-04-03 07:52:39");
    }

    #[test]
    fn test_parse_utc_time() {
        let local_time: SystemTime = Utc
            .with_ymd_and_hms(2025, 4, 3, 7, 52, 39)
            .single()
            .unwrap()
            .into();
        let time = parse_utc_time("2025-04-03 07:52:39");
        assert!(time.is_ok());
        assert_eq!(time.unwrap(), local_time);
    }
}
