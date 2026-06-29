#!/bin/sh
set -eu

raw_results_path="${WASM_RESULTS_PATH:-/tmp/kodepoint-wasm-raw-results.json}"
bmf_results_path="${BMF_RESULTS_PATH:-/tmp/kodepoint-wasm-results.json}"
benchmark_package_path="${WASM_BENCHMARK_PACKAGE:-/opt/kodepoint/wasm/kotlin/kodepoint-benchmarks-wasmJsBenchmark.mjs}"
bmf_converter_path="${WASM_BENCHMARK_BMF_CONVERTER:-/usr/local/bin/kotlinx-benchmark-to-bmf.mjs}"
config_path="$(mktemp /tmp/kodepoint-wasm-config.XXXXXX)"

cleanup() {
  rm -f "$config_path"
}

trap cleanup EXIT

mkdir -p "$(dirname "$raw_results_path")" "$(dirname "$bmf_results_path")"

cat > "$config_path" <<EOF
name:wasmJs
traceFormat:text
iterations:3
warmups:2
iterationTime:500
iterationTimeUnit:ms
compilationMode:Production
configurationName:quick
reportFile:${raw_results_path}
EOF

node "$benchmark_package_path" "$config_path"
node "$bmf_converter_path" "$raw_results_path" "$bmf_results_path"
