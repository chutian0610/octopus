use std::path::Path;

fn main() -> Result<(), String> {

    use std::io::Write;
    let out = std::path::PathBuf::from(std::env::var("OUT_DIR").unwrap());

    // for use in docker build where file changes can be wonky
    println!("cargo:rerun-if-env-changed=FORCE_REBUILD");

    let version = rustc_version::version().unwrap();
    println!("cargo:rustc-env=RUSTC_VERSION={version}");

    #[cfg(feature = "docsrs")]
    let path = out.join("octopus.rs");
    #[cfg(not(feature = "docsrs"))]
    let path = "src/serde/generated/octopus.rs";

    // We don't include the proto files in releases so that downstreams
    // do not need to have PROTOC included
    if Path::new("proto/octopus.proto").exists() {
        // println!("cargo:rerun-if-changed=proto/datafusion_common.proto");
        // println!("cargo:rerun-if-changed=proto/datafusion.proto");
        println!("cargo:rerun-if-changed=proto/octopus.proto");
        tonic_build::configure()
            // .extern_path(".datafusion_common", "::datafusion_proto_common")
            // .extern_path(".datafusion", "::datafusion_proto::protobuf")
            .protoc_arg("--experimental_allow_proto3_optional")
            .compile_protos(&["proto/octopus.proto"], &["proto"])
            .map_err(|e| format!("protobuf compilation failed: {e}"))?;
        let generated_source_path = out.join("octopus.protobuf.rs");
        let code = std::fs::read_to_string(generated_source_path).unwrap();
        let mut file = std::fs::OpenOptions::new()
            .write(true)
            .truncate(true)
            .create(true)
            .open(path)
            .unwrap();
        file.write_all(code.as_str().as_ref()).unwrap();
    }
    Ok(())
}