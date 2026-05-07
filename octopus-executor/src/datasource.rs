use datafusion::prelude::*;
use std::path::Path;
use tracing::info;
use crate::{Result, OctopusError};

#[derive(Debug, Clone, Copy)]
pub enum FileFormat {
    Parquet,
    Csv,
    Json,
}

impl FileFormat {
    pub fn from_extension(ext: &str) -> Option<Self> {
        match ext.to_lowercase().as_str() {
            "parquet" => Some(FileFormat::Parquet),
            "csv" | "tsv" => Some(FileFormat::Csv),
            "json" | "jsonl" | "ndjson" => Some(FileFormat::Json),
            _ => None,
        }
    }
}

pub struct DataSourceRegistrar {
    ctx: SessionContext,
}

impl DataSourceRegistrar {
    pub fn new(ctx: SessionContext) -> Self {
        Self { ctx }
    }

    pub async fn register_parquet(&self, name: &str, path: &str) -> Result<()> {
        info!("Registering Parquet source: {} from {}", name, path);

        self.ctx.register_parquet(
            name,
            path,
            ParquetReadOptions::default()
        )
        .await
        .map_err(|e| OctopusError::DataSourceError(e.to_string()))?;

        info!("Registered Parquet source: {}", name);
        Ok(())
    }

    pub async fn register_csv(&self, name: &str, path: &str, has_header: bool) -> Result<()> {
        info!("Registering CSV source: {} from {} (header: {})", name, path, has_header);

        let options = CsvReadOptions::new()
            .has_header(has_header);

        self.ctx.register_csv(name, path, options)
            .await
            .map_err(|e| OctopusError::DataSourceError(e.to_string()))?;

        info!("Registered CSV source: {}", name);
        Ok(())
    }

    pub async fn register_json(&self, name: &str, path: &str) -> Result<()> {
        info!("Registering JSON source: {} from {}", name, path);

        self.ctx.register_json(
            name,
            path,
            NdJsonReadOptions::default()
        )
        .await
        .map_err(|e| OctopusError::DataSourceError(e.to_string()))?;

        info!("Registered JSON source: {}", name);
        Ok(())
    }

    pub async fn register_auto(&self, name: &str, path: &str) -> Result<()> {
        let path_obj = Path::new(path);
        let extension = path_obj
            .extension()
            .and_then(|e| e.to_str())
            .unwrap_or("");

        match FileFormat::from_extension(extension) {
            Some(FileFormat::Parquet) => self.register_parquet(name, path).await,
            Some(FileFormat::Csv) => self.register_csv(name, path, true).await,
            Some(FileFormat::Json) => self.register_json(name, path).await,
            None => Err(OctopusError::DataSourceError(
                format!("Unsupported file format: {}", extension)
            )),
        }
    }

    pub async fn register_directory(&self, name: &str, dir: &str, extension: &str) -> Result<()> {
        info!("Registering directory {} as {} with extension {}", dir, name, extension);

        let path = Path::new(dir);
        if !path.is_dir() {
            return Err(OctopusError::DataSourceError(
                format!("Not a directory: {}", dir)
            ));
        }

        let glob_pattern = format!("{}/*.{}", dir, extension);

        match FileFormat::from_extension(extension) {
            Some(FileFormat::Parquet) => {
                let options = ParquetReadOptions::default();
                self.ctx.register_parquet(name, &glob_pattern, options).await
                    .map_err(|e| OctopusError::DataSourceError(e.to_string()))?
            },
            Some(FileFormat::Csv) => {
                let options = CsvReadOptions::new().has_header(true);
                self.ctx.register_csv(name, &glob_pattern, options).await
                    .map_err(|e| OctopusError::DataSourceError(e.to_string()))?
            },
            Some(FileFormat::Json) => {
                self.ctx.register_json(name, &glob_pattern, NdJsonReadOptions::default()).await
                    .map_err(|e| OctopusError::DataSourceError(e.to_string()))?
            },
            None => return Err(OctopusError::DataSourceError(
                format!("Unsupported file format: {}", extension)
            )),
        };

        info!("Registered directory {} as {}", dir, name);
        Ok(())
    }
}