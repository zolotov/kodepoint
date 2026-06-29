#!/usr/bin/env node

import { readFileSync, writeFileSync } from "node:fs";

const [inputPath, outputPath] = process.argv.slice(2);

if (!inputPath || !outputPath) {
    console.error("Usage: kotlinx-benchmark-to-bmf.mjs <input-json> <output-json>");
    process.exit(1);
}

const input = JSON.parse(readFileSync(inputPath, "utf8"));

if (!Array.isArray(input)) {
    throw new Error("Expected kotlinx-benchmark JSON to be an array.");
}

const throughputScale = (scoreUnit) => {
    switch (scoreUnit) {
        case "ops/s":
        case "ops/sec":
        case "ops/second":
            return 1;
        case "ops/ms":
            return 1e3;
        case "ops/us":
        case "ops/µs":
        case "ops/μs":
            return 1e6;
        case "ops/ns":
            return 1e9;
        case "ops/min":
            return 1 / 60;
        default:
            throw new Error(`Unsupported throughput score unit: ${scoreUnit}`);
    }
};

const benchmarkName = (entry) => {
    const params = Object.entries(entry.params ?? {});
    if (params.length === 0) {
        return entry.benchmark;
    }

    return `${entry.benchmark} | ${params.map(([key, value]) => `${key}=${value}`).join(", ")}`;
};

const result = Object.fromEntries(
    input.map((entry) => {
        if (entry.mode !== "thrpt") {
            throw new Error(`Unsupported benchmark mode: ${entry.mode}`);
        }

        const primaryMetric = entry.primaryMetric;
        if (!primaryMetric || typeof primaryMetric.score !== "number") {
            throw new Error(`Missing primaryMetric.score for benchmark: ${entry.benchmark}`);
        }

        const scale = throughputScale(primaryMetric.scoreUnit);
        const throughput = {
            value: primaryMetric.score * scale,
        };

        const confidence = Array.isArray(primaryMetric.scoreConfidence) ? primaryMetric.scoreConfidence : [];
        if (confidence.length === 2 && confidence.every((value) => typeof value === "number")) {
            throughput.lower_value = confidence[0] * scale;
            throughput.upper_value = confidence[1] * scale;
        }

        return [
            benchmarkName(entry),
            {
                throughput,
            },
        ];
    }),
);

writeFileSync(outputPath, `${JSON.stringify(result, null, 2)}\n`);
