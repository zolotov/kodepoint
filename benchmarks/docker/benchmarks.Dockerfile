# Bencher rejects the Amazon Linux Corretto image because its cacerts symlink
# traverses out of the layer path during OCI unpacking.
FROM amazoncorretto:24-alpine-jdk

WORKDIR /opt/kodepoint

COPY build/benchmarks/jvm/jars/benchmarks-jvm-jmh-JMH.jar /opt/kodepoint/benchmarks-jvm-jmh-JMH.jar
COPY docker/run-kodepoint-benchmarks.sh /usr/local/bin/run-kodepoint-benchmarks

RUN chmod +x /usr/local/bin/run-kodepoint-benchmarks

ENTRYPOINT ["/usr/local/bin/run-kodepoint-benchmarks"]
