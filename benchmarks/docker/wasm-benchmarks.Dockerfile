FROM amazoncorretto:24-alpine-jdk

RUN set -eux; \
    apk add --no-cache nodejs; \
    node --version

WORKDIR /opt/kodepoint

COPY build/wasm/packages/kodepoint-benchmarks-wasmJsBenchmark /opt/kodepoint/wasm
COPY benchmarks/docker/run-kodepoint-wasm-benchmarks.sh /usr/local/bin/run-kodepoint-wasm-benchmarks
COPY benchmarks/docker/kotlinx-benchmark-to-bmf.mjs /usr/local/bin/kotlinx-benchmark-to-bmf.mjs

RUN chmod +x /usr/local/bin/run-kodepoint-wasm-benchmarks

ENTRYPOINT ["/usr/local/bin/run-kodepoint-wasm-benchmarks"]
