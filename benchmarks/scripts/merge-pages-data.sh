#!/usr/bin/env bash
# Merges the currently published GitHub Pages data into a freshly assembled site
# directory before deployment. actions/deploy-pages always replaces the whole site,
# so every deploy has to carry forward the parts it does not regenerate itself:
# the main-branch trend data and the per-PR payloads of all other active PRs.
#
# Usage:
#   merge-pages-data.sh --site <dir> --base-url <published pages url> \
#       [--preserve-main] [--add-pr <number>] [--remove-pr <number>]
#
#   --preserve-main  Overwrite the site's data/{latest,comparison,history}.json and
#                    data/data.js with the live published versions. Used by PR deploys
#                    and close-cleanup, whose artifacts carry PR data, not main data.
#   --add-pr <n>     The site dir already contains fresh data/prs/<n>/ payloads for
#                    this PR (from the benchmark artifact); refresh its index entry.
#   --remove-pr <n>  Drop this PR from the index and do not carry its data forward.
#
# Fetch failures other than HTTP 404 abort the merge: deploying without previously
# published data would silently wipe it.
set -euo pipefail

SITE_DIR=""
BASE_URL=""
PRESERVE_MAIN=false
ADD_PR=""
REMOVE_PR=""

while [ $# -gt 0 ]; do
  case "$1" in
    --site) SITE_DIR="$2"; shift 2 ;;
    --base-url) BASE_URL="$2"; shift 2 ;;
    --preserve-main) PRESERVE_MAIN=true; shift ;;
    --add-pr) ADD_PR="$2"; shift 2 ;;
    --remove-pr) REMOVE_PR="$2"; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

[ -n "$SITE_DIR" ] && [ -d "$SITE_DIR" ] || { echo "--site must point to the assembled site directory" >&2; exit 2; }
[ -n "$BASE_URL" ] || { echo "--base-url is required" >&2; exit 2; }
BASE_URL="${BASE_URL%/}"

# fetch_optional <url> <dest> -> "ok" | "missing"; any other outcome aborts.
fetch_optional() {
  local url="$1" dest="$2" status
  status=$(curl -sSL --retry 3 --retry-all-errors -o "$dest" -w "%{http_code}" "$url") || status=000
  case "$status" in
    200) echo ok ;;
    404) rm -f "$dest"; echo missing ;;
    *) echo "::error::Failed to fetch $url (HTTP $status); aborting to avoid wiping published data." >&2; exit 1 ;;
  esac
}

DATA_DIR="$SITE_DIR/data"
PRS_DIR="$DATA_DIR/prs"
mkdir -p "$PRS_DIR"

workdir=$(mktemp -d)
trap 'rm -rf "$workdir"' EXIT

# --- live PR index -----------------------------------------------------------
live_index="$workdir/live-index.json"
if [ "$(fetch_optional "$BASE_URL/data/prs/index.json" "$live_index")" = "missing" ]; then
  echo '{"prs":[]}' > "$live_index"
  echo "No published PR index yet; starting fresh."
fi

# --- carry forward other PRs' data ------------------------------------------
entries="$workdir/entries.ndjson"
: > "$entries"

for number in $(jq -r '.prs[].number' "$live_index"); do
  if [ "$number" = "${ADD_PR:-}" ] || [ "$number" = "${REMOVE_PR:-}" ]; then
    continue
  fi
  pr_dir="$PRS_DIR/$number"
  mkdir -p "$pr_dir"
  complete=true
  for name in history comparison latest; do
    if [ "$(fetch_optional "$BASE_URL/data/prs/$number/$name.json" "$pr_dir/$name.json")" = "missing" ]; then
      complete=false
    fi
  done
  if [ "$complete" = true ]; then
    jq -c --argjson n "$number" '.prs[] | select(.number == $n)' "$live_index" >> "$entries"
  else
    echo "::warning::Published data for PR #$number is incomplete; dropping it from the index."
    rm -rf "$pr_dir"
  fi
done

if [ -n "$REMOVE_PR" ]; then
  rm -rf "$PRS_DIR/$REMOVE_PR"
  echo "Removed PR #$REMOVE_PR from the site."
fi

# --- index entry for the PR added/updated by this run ------------------------
if [ -n "$ADD_PR" ]; then
  pr_history="$PRS_DIR/$ADD_PR/history.json"
  [ -f "$pr_history" ] || { echo "--add-pr $ADD_PR given but $pr_history is missing from the site dir" >&2; exit 1; }
  jq -c --argjson n "$ADD_PR" \
    '{number: $n, refName: (.runs[-1].refName // ""), updatedAt: (.runs[-1].generatedAt // ""), runs: (.runs | length)}' \
    "$pr_history" >> "$entries"
fi

jq -s '{schemaVersion: 1, prs: (. | sort_by(.number))}' "$entries" > "$PRS_DIR/index.json"
echo "PR index now lists $(jq '.prs | length' "$PRS_DIR/index.json") pull request(s)."

# --- preserve live main-branch data ------------------------------------------
if [ "$PRESERVE_MAIN" = true ]; then
  preserved=true
  for name in latest.json comparison.json history.json data.js; do
    live_file="$workdir/main-$name"
    if [ "$(fetch_optional "$BASE_URL/data/$name" "$live_file")" = "ok" ]; then
      mv "$live_file" "$DATA_DIR/$name"
    else
      preserved=false
    fi
  done
  if [ "$preserved" = true ]; then
    echo "Preserved live main-branch data."
  else
    echo "::warning::Some main-branch data is not published yet; the artifact's own data was kept for the missing files."
  fi
fi
