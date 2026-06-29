#!/bin/sh
set -eu

results_path="${JMH_RESULTS_PATH:-/tmp/kodepoint-jmh-results.json}"

exec java \
  -Dfile.encoding=UTF-8 \
  -Duser.country=US \
  -Duser.language=en \
  -jar /opt/kodepoint/benchmarks-jvm-jmh-JMH.jar \
  -f 0 \
  -wi 2 \
  -i 5 \
  -w 250ms \
  -r 250ms \
  -bm thrpt \
  -tu us \
  -rf json \
  -rff "${results_path}"
