use std::env;
use std::path::{Path, PathBuf};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let proto_dir = "../../proto";
    println!("cargo:rerun-if-changed={}", proto_dir);

    let proto_files = vec![
        "common", 
        "health",
    ];
    let protos: Vec<String> = proto_files
        .iter()
        .map(|f| format!("{}/{}.proto", proto_dir, f))
        .collect();

    let out_dir = PathBuf::from(env::var("OUT_DIR").expect("OUT_DIR envvar is missing"));
    let file_descriptor_set_path: PathBuf = out_dir.join("file_descriptor_set.bin");

    tonic_build::configure()
        .protoc_arg("--experimental_allow_proto3_optional")
        .file_descriptor_set_path(file_descriptor_set_path)
        .out_dir(out_dir.as_path())
        .compile_protos(&protos, &[proto_dir.to_owned()])?;

    compare_and_copy(&out_dir, &PathBuf::from("./src")).unwrap_or_else(|_| {
        panic!(
            "Failed to copy generated files from {} to ./src",
            out_dir.display()
        )
    });
    Ok(())
}
/// Copy all files from `src_dir` to `dst_dir` only if they are changed.
fn compare_and_copy(src_dir: &Path, dst_dir: &Path) -> Result<(), Box<dyn std::error::Error>> {
    println!("copying files from {} to {}",src_dir.display(),dst_dir.display());
    
    let mut updated = false;
    let t1 = std::time::Instant::now();
    for entry in walkdir::WalkDir::new(src_dir) {
        let entry = entry?;
        if entry.file_type().is_file() {
            let src_file = entry.path();
            let dst_file = dst_dir.join(src_file.strip_prefix(src_dir)?);
            if dst_file.exists() {
                let src_content = fs_err::read(src_file)?;
                let dst_content = fs_err::read(dst_file.as_path())?;
                if src_content == dst_content {
                    continue;
                }
            }
            updated = true;
            println!("copying {} to {}", src_file.display(), dst_file.display());
            fs_err::create_dir_all(dst_file.parent().unwrap())?;
            fs_err::copy(src_file, dst_file)?;
        }
    }
    println!(
        "Finished generating risingwave_prost in {:?}{}",
        t1.elapsed(),
        if updated { "" } else { ", no file is updated" }
    );

    Ok(())
}