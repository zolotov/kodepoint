# Benchmarks

This module contains the `kodepoint` benchmark suite.

## Running Locally

```bash
# Full benchmarks (5 warmups, 5 iterations)
./gradlew :benchmarks:benchmark

# Quick benchmarks (2 warmups, 3 iterations)
./gradlew :benchmarks:quickBenchmark
```

## Benchmark Categories

**CodepointsBenchmark** - Measures performance across different character sets:
- ASCII characters (fast-path optimization)
- Latin Extended (U+0100-024F)
- Greek (U+0370-03FF)
- CJK ideographs (U+4E00-4E7F)
- Mixed workloads (80/20, 50/50, 20/80 ASCII/Unicode ratios)

**JvmComparisonBenchmark** - Direct comparison with `java.lang.Character`:
- `isLetter`, `isDigit`, `toLowerCase`, `toUpperCase`
- `isWhitespace`, `isJavaIdentifierStart`, `isJavaIdentifierPart`

Results are written to `benchmarks/build/reports/benchmarks/`.

## Bencher Integration

GitHub Actions builds a self-contained JVM benchmark image, pushes it to the Bencher OCI registry, and runs it on Bencher Bare Metal on pushes to `main` and on pull requests from branches in this repository.
The image definition lives under `benchmarks/docker/`.
CI builds the runnable JMH jar with Corretto 24, and the bare-metal image runs it on the Alpine-based Corretto 24 image. The Alpine variant avoids the `cacerts` symlink traversal present in the Amazon Linux image, which Bencher rejects while unpacking OCI layers.

To enable it, add:

- Repository variable `BENCHER_PROJECT` with your Bencher project slug
- Repository variable `BENCHER_USER_EMAIL` with the email address that owns your Bencher user API key
- Repository secret `BENCHER_API_KEY` with a Bencher user API key (`bencher_user_*`)

The Bencher Bare Metal run uses a dedicated `intel-v1-corretto24-jmh-nonfork` testbed and a reduced non-forked JMH profile so the full suite fits within the Bencher Free tier's 5 minute job timeout.

Closed pull requests automatically archive their Bencher branch. Fork pull requests do not upload results because GitHub does not expose repository secrets to them.
