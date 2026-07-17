# Benchmarks

This module contains the `kodepoint` benchmark suite and the Gradle-owned reporting
pipeline that replaces the previous Bencher setup. CI is intentionally a thin shell:
everything that produces, compares, or renders data lives in
`buildSrc/src/main/kotlin/me/zolotov/kodepoint/gradle/BenchmarkReportTask.kt` and the
static dashboard under `benchmarks/site/`, so the whole thing is portable to any CI
that can run `./gradlew` and publish a directory of static files.

## Running Locally

```bash
# Quick benchmarks (2 warmups, 3 iterations)
./gradlew :benchmarks:jvmQuickBenchmark
./gradlew :benchmarks:wasmJsQuickBenchmark

# Full benchmarks (5 warmups, 5 iterations)
./gradlew :benchmarks:benchmark

# CharacterData size metrics
./gradlew :unicode:characterDataMetrics

# CI-style aggregate report + Pages bundle
./gradlew :benchmarks:ciBenchmark

# Compare against the published history (what CI does)
curl -fsSL https://zolotov.github.io/kodepoint/data/history.json -o /tmp/history.json
./gradlew :benchmarks:ciBenchmark \
  -PbenchmarkHistoryFile=/tmp/history.json \
  -PbenchmarkSiteUrl=https://zolotov.github.io/kodepoint
```

After `ciBenchmark`, open `benchmarks/build/ci/site/index.html` directly in a browser —
the dashboard embeds its data in `data/data.js`, so it works from `file://` without a
web server. `benchmarks/build/ci/report/summary.md` is the markdown version of the same
comparison.

Gradle properties understood by `ciBenchmark`:

| Property                         | Default | Meaning                                                                                                                                                                                                                                               |
|----------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `benchmarkHistoryFile`           | none    | Existing `history.json` to use as the comparison baseline and to extend                                                                                                                                                                               |
| `benchmarkSiteUrl`               | none    | Published Pages URL, embedded into reports and the step summary                                                                                                                                                                                       |
| `benchmarkPrNumber`              | none    | Pull request number. Enables the per-PR site payload (`site/data/prs/<n>/`), `report/pr-history.json`, and the "Benchmark Runs in This PR" summary section                                                                                            |
| `benchmarkPrHistoryFile`         | none    | Previously published run history for this PR; the current run is appended to it. `benchmarkHistoryFile` stays the comparison baseline                                                                                                                 |
| `benchmarkHistoryLimit`          | `90`    | Number of runs retained in `history.json` (and per-PR histories)                                                                                                                                                                                      |
| `benchmarkSignificanceThreshold` | `0.03`  | Fallback noise threshold for measurements without confidence intervals. When both runs carry JMH 99.9% confidence intervals (the normal case), significance is decided per benchmark by interval overlap instead; size metrics always compare exactly |

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
The aggregate CI report, summary markdown, raw JSON payloads, and Pages bundle are written to `benchmarks/build/ci/`:

```
benchmarks/build/ci/
├── raw/              # untouched kotlinx-benchmark + size-metric reports
├── report/
│   ├── current.json     # this run, normalized schema
│   ├── comparison.json  # per-measurement delta vs the baseline
│   ├── history.json     # bounded run history (baseline + this run)
│   ├── pr-history.json  # PR runs only (PR runs; previous PR history + this run)
│   └── summary.md       # markdown summary (also appended to the GitHub step summary)
└── site/             # deployable static dashboard (template + data)
    └── data/prs/<n>/ # per-PR payload (PR runs; same three-file contract as data/)
```

CharacterData size metrics are written to `unicode/build/reports/character-data/metrics.json`.

## GitHub Workflow

`.github/workflows/benchmarks.yml` keeps the workflow intentionally thin:

- install Java, Node, and Gradle
- download the published `history.json` from the existing Pages site (404 = fresh start;
  any other failure aborts the run so a transient outage can never wipe the trend data);
  for PR runs, additionally download that PR's published run history
- run `./gradlew :benchmarks:ciBenchmark`
- upload `benchmarks/build/ci/` as the workflow artifact
- on pull requests, upsert a sticky PR comment with `report/summary.md`
- deploy to GitHub Pages after every `main` push and every non-fork PR run; on PR close,
  redeploy without that PR's data (see "Pull requests on Pages" below)

The Gradle task owns the rest:

- running `jvmQuickBenchmark`, `wasmJsQuickBenchmark`, and `:unicode:characterDataMetrics`
- normalizing results into one JSON schema
- comparing the current run to the latest published baseline (per-benchmark confidence-interval overlap decides significance)
- writing `summary.md` and appending it to `GITHUB_STEP_SUMMARY` when present
- assembling the static dashboard (`benchmarks/site/` template + generated `data/`)

### Pull requests

PR runs compare against the latest run published from `main` and surface the result in
four places: the sticky PR comment, the workflow step summary, the
`benchmark-report-*` artifact (which contains the full site — download it and open
`site/index.html` to browse a PR run interactively), and the published Pages dashboard
(`…/?pr=<number>`). PR results are never merged into the main trend history.

Every push to a PR appends a run to that PR's own history, so the sticky comment's
"Benchmark Runs in This PR" table and the PR dashboard's trend charts cover all of the
PR's runs, each compared against the same main baseline.

Fork PRs run on `ubuntu-latest` regardless of `BENCHMARK_RUNNER` (untrusted code must
not execute on a persistent self-hosted machine), skip the PR comment because the
fork token is read-only, and never publish to Pages (no OIDC token for fork events).

### Pull requests on Pages

The dashboard's main view lists active PRs (from `data/prs/index.json`); `?pr=<n>`
renders the standard dashboard from `data/prs/<n>/{latest,comparison,history}.json`,
where `history.json` holds only that PR's runs.

`actions/deploy-pages` always replaces the entire site, so every deploy first runs
`benchmarks/scripts/merge-pages-data.sh`, which carries forward whatever the run did
not produce itself: PR deploys fetch the live main-branch data, main deploys fetch all
live PR data, and both preserve the other PRs. Deploys are serialized through the
`benchmark-pages-deploy` concurrency group. Any fetch failure other than HTTP 404
aborts the deploy rather than silently dropping published data. (GitHub keeps at most
one pending job per concurrency group, so under heavy parallel PR traffic a PR's deploy
can occasionally be superseded; its data reappears with that PR's next run.)

When a PR closes, a cleanup job assembles the site from the template plus live data
minus the closed PR and redeploys, so `data/prs/` only ever holds open PRs.

### One-time repository setup

1. Settings → Pages → Build and deployment → Source: **GitHub Actions**.
2. Settings → Environments → `github-pages` → Deployment branches and tags:
   **No restriction**. PR-triggered deploys present the merge ref
   (`refs/pull/<n>/merge`), which deployment branch policies can never match (they
   only apply to real branches — patterns like `pull/*/merge` were tried and are
   rejected at deploy time), so the default main-only policy blocks PR deploys.
   Equivalent CLI:
   ```bash
   gh api -X PUT repos/{owner}/{repo}/environments/github-pages \
     --input - <<< '{"deployment_branch_policy": null}'
   ```
   This is safe here: only the benchmarks workflow requests `pages: write`, and fork
   PRs never reach the deploy job nor get an OIDC token.
2. Optional repository variables (Settings → Secrets and variables → Actions → Variables):
   - `BENCHMARK_RUNNER` — label of a self-hosted runner for stable numbers
     (see below). Falls back to `ubuntu-latest`.
   - `BENCHMARK_PAGES_URL` — override when using a custom domain. Defaults to
     `https://zolotov.github.io/kodepoint`.
   - `GCP_RUNNER_WIF_PROVIDER`, `GCP_RUNNER_SERVICE_ACCOUNT`, `GCP_RUNNER_INSTANCE` —
     enable on-demand start of the GCE runner (see "On-demand start and stop" below).
     When unset, the start-runner job is skipped and benchmarks run as before.

### Viewing the data

- Dashboard: `https://zolotov.github.io/kodepoint` — current snapshot, kodepoint vs
  `java.lang.Character` comparison, sparklines, and expandable per-benchmark history
  charts. The "Largest Regressions/Improvements" panels appear only on PR and local
  seeded runs (where a baseline comparison is the point); the published main dashboard
  relies on the trend charts instead.
- Machine-readable JSON next to it: `…/data/latest.json`, `…/data/comparison.json`,
  `…/data/history.json` (schema version 1; `history.json` is also the seed the next
  run consumes, so it is the canonical trend store).
- Active PRs: listed on the main dashboard; each links to `…/?pr=<number>` backed by
  `…/data/prs/<number>/{latest,comparison,history}.json` and indexed in
  `…/data/prs/index.json`.
- Per-run: workflow step summary and the uploaded `benchmark-report-*` artifact.

## Self-hosted benchmark runner on Google Compute Engine

GitHub-hosted runners are shared VMs; their numbers are noisy across runs. For stable
trends, register a dedicated GCE instance as a self-hosted runner and point
`BENCHMARK_RUNNER` at it. The workflow needs nothing preinstalled beyond `curl` and
`git` — the `setup-java` / `setup-node` / `setup-gradle` actions provision toolchains
into the runner's tool cache.

1. Create the instance (a fixed machine type; avoid shared-core `e2-micro`/`e2-small`,
   which burst and ruin comparability):

   ```bash
   gcloud compute instances create kodepoint-bench \
     --zone=europe-west1-b \
     --machine-type=c2d-standard-4 \
     --image-family=ubuntu-2404-lts-amd64 \
     --image-project=ubuntu-os-cloud \
     --boot-disk-size=50GB
   ```

2. Install the runner (repo Settings → Actions → Runners → New self-hosted runner
   shows the exact commands with a fresh registration token):

   ```bash
   gcloud compute ssh kodepoint-bench --zone=europe-west1-b
   sudo apt-get update && sudo apt-get install -y curl git
   mkdir actions-runner && cd actions-runner
   curl -o actions-runner.tar.gz -L https://github.com/actions/runner/releases/download/v<version>/actions-runner-linux-x64-<version>.tar.gz
   tar xzf actions-runner.tar.gz
   sudo ./bin/installdependencies.sh   # the runner is .NET-based; installs libicu etc.
   ./config.sh --url https://github.com/zolotov/kodepoint \
     --token <registration-token> \
     --labels gce-benchmark \
     --unattended
   sudo ./svc.sh install && sudo ./svc.sh start   # run as a systemd service
   ```

   Registration tokens expire after ~1 hour; if `config.sh` fails with an auth error,
   generate a fresh token from the same settings page.

3. Set the repository variable `BENCHMARK_RUNNER` to `gce-benchmark`. The next
   workflow run picks the machine up; no workflow changes needed. To switch back to
   GitHub-hosted runners, delete the variable.

Notes:

- Keep exactly one benchmark runner per label — two machines with the same label would
  interleave and produce incomparable numbers. Note that switching machine types resets
  comparability of the trend history for the same reason.
- Security: fork PRs are already routed away from the self-hosted runner by the
  workflow. Additionally set Settings → Actions → General → Fork pull request
  workflows to "Require approval for all outside collaborators".
- GCE bills per second while the instance is in `RUNNING` state, working or idle
  (a stopped instance costs only its disk, a few dollars per month). Set up the
  on-demand start/stop below so the machine only runs around actual jobs.

### On-demand start and stop

With both pieces in place the instance starts when a benchmark job is triggered and
stops itself 30 minutes after the last job — for a few runs a day that is dollars,
not hundreds, per month.

**1. Auto-stop when idle (on the VM).** `Runner.Worker` only exists while a job is
executing, so a timer can power the VM off after 30 job-free minutes. Powering off
from inside the guest moves the instance to `TERMINATED`, which ends compute billing.
Paste over SSH:

```bash
sudo tee /usr/local/bin/benchmark-idle-shutdown >/dev/null <<'EOF'
#!/bin/bash
# Stop the VM after 30 minutes without a running GitHub Actions job.
STAMP=/run/benchmark-last-activity
IDLE_LIMIT=1800
pgrep -f Runner.Worker >/dev/null && touch "$STAMP"
[ -f "$STAMP" ] || touch "$STAMP"   # /run is cleared on boot: grace period starts here
idle=$(( $(date +%s) - $(stat -c %Y "$STAMP") ))
if [ "$idle" -gt "$IDLE_LIMIT" ]; then
    logger -t idle-shutdown "No benchmark job for ${idle}s; powering off."
    systemctl poweroff
fi
EOF
sudo chmod +x /usr/local/bin/benchmark-idle-shutdown

sudo tee /etc/systemd/system/benchmark-idle-shutdown.service >/dev/null <<'EOF'
[Unit]
Description=Stop the VM when no benchmark job has run recently

[Service]
Type=oneshot
ExecStart=/usr/local/bin/benchmark-idle-shutdown
EOF

sudo tee /etc/systemd/system/benchmark-idle-shutdown.timer >/dev/null <<'EOF'
[Unit]
Description=Periodic idle check for the benchmark runner

[Timer]
OnBootSec=5min
OnUnitActiveSec=5min

[Install]
WantedBy=timers.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now benchmark-idle-shutdown.timer
systemctl list-timers benchmark-idle-shutdown.timer   # sanity check
```

The 30-minute tail means consecutive pushes reuse the warm machine. Rare race to be
aware of: if the VM powers off in the same instant a new job is assigned, that job
stays queued — re-run the workflow (it starts the instance again).

**2. Auto-start from the workflow (keyless, via Workload Identity Federation).**
The `start-runner` job in `benchmarks.yml` calls the Compute API to start the
instance before the benchmark job runs; it activates only when the repository
variables below are set. One-time GCP setup (Cloud Shell or any authenticated
`gcloud`):

```bash
PROJECT_ID=$(gcloud config get-value project)
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
REPO=zolotov/kodepoint
#ZONE=<zone>
#INSTANCE=<instance-name>

# Needed to mint short-lived tokens for the impersonated service account
gcloud services enable iamcredentials.googleapis.com

# Service account that is only allowed to start/stop this one instance
gcloud iam service-accounts create gh-benchmark-starter \
  --display-name="GitHub Actions benchmark runner starter"
gcloud compute instances add-iam-policy-binding "$INSTANCE" --zone="$ZONE" \
  --member="serviceAccount:gh-benchmark-starter@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/compute.instanceAdmin.v1"

# Identity pool + provider trusting GitHub's OIDC tokens for this repository only
gcloud iam workload-identity-pools create github --location=global
gcloud iam workload-identity-pools providers create-oidc github-actions \
  --location=global --workload-identity-pool=github \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='${REPO}'"

# Allow the repo's workflows to impersonate the service account
gcloud iam service-accounts add-iam-policy-binding \
  "gh-benchmark-starter@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/attribute.repository/${REPO}"

echo "GCP_RUNNER_WIF_PROVIDER=projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/providers/github-actions"
echo "GCP_RUNNER_SERVICE_ACCOUNT=gh-benchmark-starter@${PROJECT_ID}.iam.gserviceaccount.com"
echo "GCP_RUNNER_INSTANCE=projects/${PROJECT_ID}/zones/${ZONE}/instances/${INSTANCE}"
```

Set the three echoed values as repository variables (Settings → Secrets and
variables → Actions → Variables). No key files or secrets are involved — the
workflow exchanges GitHub's OIDC token for a short-lived GCP token at run time.

Important: starting an instance that has an attached service account additionally
requires `iam.serviceAccounts.actAs`. The cleanest fix is to remove the service
account from the runner VM — it should not hold cloud credentials anyway, since it
executes repository code:

```bash
gcloud compute instances stop "$INSTANCE" --zone="$ZONE"
gcloud compute instances set-service-account "$INSTANCE" --zone="$ZONE" \
  --no-service-account --no-scopes
gcloud compute instances start "$INSTANCE" --zone="$ZONE"
```

<!-- smoke test: PR Pages deploy; safe to delete -->
