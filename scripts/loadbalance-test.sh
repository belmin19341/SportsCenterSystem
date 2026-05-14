#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# SportsCenterSystem — concurrent load-balancer benchmark
#
# Sends a concurrent burst of requests through the API Gateway to the non-trivial:
#   GET /api/pricing-rules/calculate
#
# The gateway route uses lb://resource-service, so when two Resource Service
# instances are running Eureka + Spring Cloud LoadBalancer should distribute the
# traffic automatically. Each upstream instance stamps X-Instance-Id, which is
# preserved through the gateway and recorded in the output.
#
# Example:
#   ./scripts/loadbalance-test.sh --scenario two-instances
#   ./scripts/loadbalance-test.sh --scenario single-instance --requests 100
# ═══════════════════════════════════════════════════════════════════════════════
set -euo pipefail

if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${RESULTS_DIR:-$SCRIPT_DIR/loadbalance-results/latest}"
GATEWAY="${GATEWAY:-http://localhost:${API_GATEWAY_PORT:-8080}}"
LOGIN_URL="${LOGIN_URL:-$GATEWAY/api/auth/login}"
SCENARIO="adhoc"
REQUESTS=100
CONCURRENCY=""
FACILITY_ID="${FACILITY_ID:-1}"
START_AT="${START_AT:-2030-06-15T19:00:00}"
END_AT="${END_AT:-2030-06-15T21:00:00}"
USER_NAME="${USER_NAME:-john_doe}"
USER_PASS="${USER_PASS:-password123}"
AUTH_TOKEN="${AUTH_TOKEN:-}"

usage() {
    cat <<'EOF'
Usage: ./scripts/loadbalance-test.sh [options]

Options:
  --scenario NAME        Scenario label written into output files
  --requests N           Number of requests to send (default: 100)
  --concurrency N        Parallel workers (default: same as requests)
  --results-dir PATH     Directory for CSV/Markdown output
  --gateway URL          Gateway base URL (default: http://localhost:8080)
  --login-url URL        Login endpoint URL (default: <gateway>/api/auth/login)
  --facility-id ID       Facility ID for pricing calculation (default: 1)
  --start ISO_DATETIME   Start timestamp passed to pricing-rules/calculate
  --end ISO_DATETIME     End timestamp passed to pricing-rules/calculate
  --user NAME            Login username (default: john_doe)
  --password PASS        Login password (default: password123)
  --token TOKEN          Reuse an existing bearer token instead of logging in
  --help                 Show this help text
EOF
}

json_get() {
    local key="$1"
    local body="$2"
    echo "$body" | sed -n 's/.*"'"$key"'":[ ]*"\([^"]*\)".*/\1/p' | head -1
}

slugify() {
    echo "$1" | tr '[:upper:]' '[:lower:]' | tr ' /' '--' | tr -cd 'a-z0-9._-'
}

now_ms() {
    python3 - <<'PY'
import time
print(int(time.time() * 1000))
PY
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "❌ Missing required command: $1"
        exit 1
    }
}

warmup_request() {
    local attempts=5
    local last_status=""
    local i

    for i in $(seq 1 "$attempts"); do
        last_status=$(curl -sS -o "$TMP_DIR/warmup.json" -D "$TMP_DIR/warmup.headers" -w '%{http_code}' \
            -H "Authorization: Bearer $AUTH_TOKEN" \
            "$URL")
        if [ "$last_status" = "200" ]; then
            return 0
        fi
        sleep 2
    done

    echo "❌ Warm-up request failed with HTTP $last_status"
    cat "$TMP_DIR/warmup.json"
    return 1
}

while [ $# -gt 0 ]; do
    case "$1" in
        --scenario)
            SCENARIO="$2"
            shift 2
            ;;
        --requests)
            REQUESTS="$2"
            shift 2
            ;;
        --concurrency)
            CONCURRENCY="$2"
            shift 2
            ;;
        --results-dir)
            RESULTS_DIR="$2"
            shift 2
            ;;
        --gateway)
            GATEWAY="$2"
            shift 2
            ;;
        --login-url)
            LOGIN_URL="$2"
            shift 2
            ;;
        --facility-id)
            FACILITY_ID="$2"
            shift 2
            ;;
        --start)
            START_AT="$2"
            shift 2
            ;;
        --end)
            END_AT="$2"
            shift 2
            ;;
        --user)
            USER_NAME="$2"
            shift 2
            ;;
        --password)
            USER_PASS="$2"
            shift 2
            ;;
        --token)
            AUTH_TOKEN="$2"
            shift 2
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            echo "❌ Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

require_command curl
require_command awk
require_command xargs
require_command python3

if [ -z "$CONCURRENCY" ]; then
    CONCURRENCY="$REQUESTS"
fi

SCENARIO_SLUG="$(slugify "$SCENARIO")"
[ -n "$SCENARIO_SLUG" ] || SCENARIO_SLUG="adhoc"
mkdir -p "$RESULTS_DIR"

RAW_CSV="$RESULTS_DIR/${SCENARIO_SLUG}-raw.csv"
SUMMARY_MD="$RESULTS_DIR/${SCENARIO_SLUG}-summary.md"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

URL="${GATEWAY}/api/pricing-rules/calculate?facilityId=${FACILITY_ID}&start=${START_AT}&end=${END_AT}"

if [ -z "$AUTH_TOKEN" ]; then
    LOGIN_STATUS=$(curl -sS -o "$TMP_DIR/login.json" -w '%{http_code}' \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$USER_NAME\",\"password\":\"$USER_PASS\"}" \
        "$LOGIN_URL")

    if [ "$LOGIN_STATUS" != "200" ]; then
        echo "❌ Login failed with HTTP $LOGIN_STATUS"
        cat "$TMP_DIR/login.json"
        exit 1
    fi

    AUTH_TOKEN=$(json_get access_token "$(cat "$TMP_DIR/login.json")")
    if [ -z "$AUTH_TOKEN" ]; then
        echo "❌ access_token missing from login response"
        cat "$TMP_DIR/login.json"
        exit 1
    fi
fi

warmup_request

export AUTH_TOKEN URL TMP_DIR

START_MS="$(now_ms)"
seq "$REQUESTS" | xargs -P "$CONCURRENCY" -I{} bash -c '
    idx="$1"
    headers="$TMP_DIR/headers-$idx.txt"
    body="$TMP_DIR/body-$idx.json"
    result_file="$TMP_DIR/result-$idx.csv"
    curl_result="$(
        curl -sS -D "$headers" -o "$body" \
            -w "%{http_code},%{time_total}" \
            -H "Authorization: Bearer $AUTH_TOKEN" \
            "$URL" 2>"$TMP_DIR/curl-$idx.stderr"
    )" || curl_rc=$?

    if [ "${curl_rc:-0}" -ne 0 ]; then
        printf "%s,%s,%s,%s\n" "$idx" "curl-${curl_rc}" "0" "curl-error" > "$result_file"
        exit 0
    fi

    status="${curl_result%%,*}"
    total="${curl_result#*,}"
    instance="$(awk -F": " "/^X-Instance-Id:/ {print \$2}" "$headers" | tr -d "\r")"
    if [ -z "$instance" ]; then
        instance="missing"
    fi
    printf "%s,%s,%s,%s\n" "$idx" "$status" "$total" "$instance" > "$result_file"
' _ {}
END_MS="$(now_ms)"
WALL_MS=$((END_MS - START_MS))

{
    echo "request_id,http_status,time_total_seconds,instance_id"
    cat "$TMP_DIR"/result-*.csv | sort -t, -k1,1n
} > "$RAW_CSV"

python3 - "$RAW_CSV" "$SUMMARY_MD" "$SCENARIO" "$URL" "$REQUESTS" "$CONCURRENCY" "$WALL_MS" <<'PY'
import csv
import math
import sys
from collections import Counter
from pathlib import Path

raw_csv, summary_md, scenario, url, expected_requests, concurrency, wall_ms = sys.argv[1:]
expected_requests = int(expected_requests)
concurrency = int(concurrency)
wall_ms = int(wall_ms)

rows = []
with open(raw_csv, newline="") as fh:
    reader = csv.DictReader(fh)
    for row in reader:
        row["time_total_seconds"] = float(row["time_total_seconds"])
        rows.append(row)

success_rows = [r for r in rows if r["http_status"] == "200"]
failure_rows = [r for r in rows if r["http_status"] != "200"]
times = [r["time_total_seconds"] for r in rows]
success_times = [r["time_total_seconds"] for r in success_rows]
distribution = Counter(r["instance_id"] for r in success_rows)

def avg(values):
    return sum(values) / len(values) if values else 0.0

avg_latency_ms = avg(success_times) * 1000
min_latency_ms = (min(success_times) * 1000) if success_times else 0.0
max_latency_ms = (max(success_times) * 1000) if success_times else 0.0
throughput_rps = (len(success_rows) / (wall_ms / 1000.0)) if wall_ms else 0.0

summary_path = Path(summary_md)
summary_path.parent.mkdir(parents=True, exist_ok=True)

lines = [
    f"# Load-balancer benchmark — {scenario}",
    "",
    f"- Gateway URL: `{url}`",
    f"- Requests requested: {expected_requests}",
    f"- Parallelism: {concurrency}",
    f"- Successful responses: {len(success_rows)}",
    f"- Failed responses: {len(failure_rows)}",
    f"- Wall-clock duration: {wall_ms} ms",
    f"- Average latency (successful requests): {avg_latency_ms:.2f} ms",
    f"- Min latency: {min_latency_ms:.2f} ms",
    f"- Max latency: {max_latency_ms:.2f} ms",
    f"- Throughput: {throughput_rps:.2f} req/s",
    f"- Unique upstream instances seen: {len(distribution)}",
    "",
    "## Upstream distribution",
    "",
    "| Instance | Hits | Share |",
    "| --- | ---: | ---: |",
]

for instance, hits in distribution.most_common():
    share = (hits / len(success_rows) * 100.0) if success_rows else 0.0
    lines.append(f"| `{instance}` | {hits} | {share:.1f}% |")

if not distribution:
    lines.append("| `none` | 0 | 0.0% |")

if failure_rows:
    fail_counter = Counter(r["http_status"] for r in failure_rows)
    lines.extend([
        "",
        "## Failure breakdown",
        "",
        "| Status | Count |",
        "| --- | ---: |",
    ])
    for status, count in fail_counter.most_common():
        lines.append(f"| `{status}` | {count} |")

lines.extend([
    "",
    "## Interpretation",
    "",
])

if len(distribution) > 1:
    lines.append("- Requests were distributed across multiple `resource-service` instances, so gateway-level load balancing is active.")
else:
    lines.append("- Only one upstream instance handled the successful requests, which is expected in the single-instance scenario and a red flag in the two-instance scenario.")

if success_rows:
    lines.append("- The raw per-request measurements are stored in the adjacent CSV file for deeper analysis or charting.")

summary_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY

echo ""
echo "✓ Scenario: $SCENARIO"
echo "  Raw data: $RAW_CSV"
echo "  Summary:  $SUMMARY_MD"
