use chrono::prelude::*;
use chrono_tz::{ParseError, Tz};
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

pub fn format_local_time(st: SystemTime) -> String {
    let local_datetime: DateTime<Local> = st.into();
    local_datetime.format(FORMAT_PATTERN_DATETIME).to_string()
}

pub fn format_local_time_with_pattern(st: SystemTime, pattern: &str) -> String {
    let local_datetime: DateTime<Local> = st.into();
    local_datetime.format(pattern).to_string()
}

pub fn parse_local_time(datetime_str: &str, time_zone_str: &str) -> Result<SystemTime, String> {
    let tz: Tz = time_zone_str
        .parse()
        .map_err(|e: ParseError| format!("parsing time zone string failed: {}", e))?;
    let naive_datetime = NaiveDateTime::parse_from_str(datetime_str, FORMAT_PATTERN_DATETIME)
        .map_err(|e| format!("parsing date string failed: {}", e))?;
    let local_datetime = Local
        .from_local_datetime(&naive_datetime)
        .single()
        .map(|time| time.with_timezone(&tz))
        .ok_or_else(|| "cannot be converted to local time".to_string())?;
    Ok(local_datetime.into())
}

pub fn parse_local_time_with_pattern(
    datetime_str: &str,
    time_zone_str: &str,
    pattern: &str,
) -> Result<SystemTime, String> {
    let tz: Tz = time_zone_str
        .parse()
        .map_err(|e: ParseError| format!("parsing time zone string failed: {}", e))?;
    let naive_datetime = NaiveDateTime::parse_from_str(datetime_str, pattern)
        .map_err(|e| format!("parsing date string failed: {}", e))?;
    let local_datetime = Local
        .from_local_datetime(&naive_datetime)
        .single()
        .map(|time| time.with_timezone(&tz))
        .ok_or_else(|| "cannot be converted to local time".to_string())?;
    Ok(local_datetime.into())
}

pub fn format_utc_time(st: SystemTime) -> String {
    let utc_datetime: DateTime<Utc> = st.into();
    utc_datetime.format(FORMAT_PATTERN_DATETIME).to_string()
}
pub fn format_utc_time_with_pattern(st: SystemTime, pattern: &str) -> String {
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
    #[test]
    fn test_parse_local_time() {
        let tz: Tz = "Asia/Shanghai".parse().unwrap();
        let local_time = Utc
            .with_ymd_and_hms(2025, 4, 3, 7, 52, 39)
            .single()
            .unwrap()
            .with_timezone(&tz);
        let time = parse_local_time("2025-04-03 07:52:39", "Asia/Shanghai");
        let time2: SystemTime = local_time.into();
        assert_eq!(time.unwrap(), time2);
    }
}
