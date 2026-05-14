#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# SportsCenterSystem — compare single-instance vs two-instance load balancing
#
# Runs the stack in both modes and benchmarks the same non-trivial endpoint
# through the API Gateway:
#   1) two Resource Service instances
#   2) one Resource Service instance
#
# Output:
#   scripts/loadbalance-results/<timestamp>/
#     - two-instances-raw.csv
#     - two-instances-summary.md
#     - single-instance-raw.csv
#     - single-instance-summary.md
#     - comparison.md
# ═══════════════════════════════════════════════════════════════════════════════
set -euo pipefail

if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REQUESTS=100
CONCURRENCY=100
RUN_ID="$(date +"%Y%m%d-%H%M%S")"
RESULTS_DIR="${RESULTS_DIR:-$SCRIPT_DIR/loadbalance-results/$RUN_ID}"

usage() {
    cat <<'EOF'
Usage: ./scripts/loadbalance-compare.sh [options]

Options:
  --requests N        Number of requests per scenario (default: 100)
  --concurrency N     Parallel workers per scenario (default: 100)
  --results-dir PATH  Output directory
  --help              Show this help text
EOF
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "❌ Missing required command: $1"
        exit 1
    }
}

restart_gateway() {
    local run_services_args=("$@")
    local gateway_port="${API_GATEWAY_PORT:-8080}"
    local pids=""

    pids=$(lsof -ti tcp:"$gateway_port" -sTCP:LISTEN 2>/dev/null || true)
    if [ -n "$pids" ]; then
        echo "   restartam api-gateway da osvježi Eureka registry ..."
        for pid in $pids; do
            kill "$pid"
        done
    fi

    if [ "${#run_services_args[@]}" -gt 0 ]; then
        (cd "$ROOT_DIR" && bash run-services.sh --backend-only "${run_services_args[@]}")
    else
        (cd "$ROOT_DIR" && bash run-services.sh --backend-only)
    fi
}

wait_for_resource_instances() {
    local expected="$1"
    local tries=90
    local count=""
    local body=""

    echo -n "   čekam $expected registrovan(ih) resource-service instanci u Eureki ..."
    for _ in $(seq 1 "$tries"); do
        body=$(curl -sS "http://localhost:${DISCOVERY_SERVER_PORT:-8761}/eureka/apps/RESOURCE-SERVICE" || true)
        count=$(printf "%s" "$body" | grep -o "<instance>" | wc -l | tr -d ' ')
        if [ "$count" = "$expected" ]; then
            echo " OK"
            return 0
        fi
        echo -n "."
        sleep 1
    done
    echo " ❌"
    echo "Expected $expected resource-service instance(s) in Eureka, but saw: ${count:-unknown}"
    return 1
}

while [ $# -gt 0 ]; do
    case "$1" in
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

require_command bash
require_command python3

mkdir -p "$RESULTS_DIR"

echo "🧪 Scenario 1/2: two Resource Service instances"
(cd "$ROOT_DIR" && bash run-services.sh --backend-only --lb)
wait_for_resource_instances 2
restart_gateway --lb
(cd "$ROOT_DIR" && bash scripts/loadbalance-test.sh \
    --scenario two-instances \
    --requests "$REQUESTS" \
    --concurrency "$CONCURRENCY" \
    --results-dir "$RESULTS_DIR")

echo ""
echo "🧪 Scenario 2/2: single Resource Service instance"
(cd "$ROOT_DIR" && bash run-services.sh --backend-only)
wait_for_resource_instances 1
restart_gateway
(cd "$ROOT_DIR" && bash scripts/loadbalance-test.sh \
    --scenario single-instance \
    --requests "$REQUESTS" \
    --concurrency "$CONCURRENCY" \
    --results-dir "$RESULTS_DIR")

python3 - "$RESULTS_DIR/two-instances-raw.csv" "$RESULTS_DIR/single-instance-raw.csv" "$RESULTS_DIR/comparison.md" <<'PY'
import csv
import sys
from collections import Counter
from pathlib import Path

two_csv, single_csv, out_md = sys.argv[1:]

def read_metrics(path):
    with open(path, newline="") as fh:
        rows = list(csv.DictReader(fh))
    success = [r for r in rows if r["http_status"] == "200"]
    failures = [r for r in rows if r["http_status"] != "200"]
    latencies = [float(r["time_total_seconds"]) * 1000.0 for r in success]
    distribution = Counter(r["instance_id"] for r in success)
    return {
        "requests": len(rows),
        "success": len(success),
        "failures": len(failures),
        "success_rate": ((len(success) / len(rows)) * 100.0) if rows else 0.0,
        "avg_ms": (sum(latencies) / len(latencies)) if latencies else 0.0,
        "min_ms": min(latencies) if latencies else 0.0,
        "max_ms": max(latencies) if latencies else 0.0,
        "instances": len(distribution),
        "distribution": distribution,
    }

two = read_metrics(two_csv)
single = read_metrics(single_csv)

def pct_change(new, old):
    if old == 0:
        return None
    return ((new - old) / old) * 100.0

latency_delta = pct_change(two["avg_ms"], single["avg_ms"])

lines = [
    "# Load-balancing comparison report",
    "",
    "## Expected result",
    "",
    "- With **two** Resource Service instances, the gateway should distribute traffic across both instances and reduce pressure on any single instance.",
    "- With **one** Resource Service instance, every request should hit the same upstream instance, so there is no balancing benefit to observe.",
    "",
    "## Observed result",
    "",
    "| Scenario | Requests | Success | Failure | Success rate | Avg latency* | Min | Max | Unique instances |",
    "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |",
    f"| Two instances | {two['requests']} | {two['success']} | {two['failures']} | {two['success_rate']:.1f}% | {two['avg_ms']:.2f} ms | {two['min_ms']:.2f} ms | {two['max_ms']:.2f} ms | {two['instances']} |",
    f"| Single instance | {single['requests']} | {single['success']} | {single['failures']} | {single['success_rate']:.1f}% | {single['avg_ms']:.2f} ms | {single['min_ms']:.2f} ms | {single['max_ms']:.2f} ms | {single['instances']} |",
    "",
    "## Instance distribution",
    "",
]

for label, metrics in (("Two instances", two), ("Single instance", single)):
    lines.append(f"### {label}")
    lines.append("")
    lines.append("| Instance | Hits | Share |")
    lines.append("| --- | ---: | ---: |")
    total = metrics["success"]
    if total == 0:
        lines.append("| `none` | 0 | 0.0% |")
    else:
        for instance, hits in metrics["distribution"].most_common():
            share = hits / total * 100.0
            lines.append(f"| `{instance}` | {hits} | {share:.1f}% |")
    lines.append("")

analysis = []
if two["instances"] > 1:
    analysis.append("- The two-instance run proves that gateway-level `lb://resource-service` routing is distributing traffic across multiple Eureka-registered instances.")
else:
    analysis.append("- The two-instance run did **not** spread requests across multiple instances, so load balancing is not behaving as expected and should be investigated.")

if single["instances"] == 1:
    analysis.append("- The single-instance run behaved as expected: all successful requests stayed on one upstream instance.")
else:
    analysis.append("- The single-instance run touched multiple upstream instances, which means the environment was not reduced to one active Resource Service instance.")

if two["failures"] == 0 and single["failures"] > 0:
    analysis.append(f"- Under the 100-request burst, the single-instance scenario dropped {single['failures']} requests while the two-instance scenario completed all 100. In this run, load balancing improved **stability/capacity** more clearly than raw latency.")
elif two["failures"] < single["failures"]:
    analysis.append(f"- The two-instance scenario reduced failures from {single['failures']} to {two['failures']}, which shows a clear resilience benefit under concurrent load.")
elif latency_delta is None:
    analysis.append("- Average-latency comparison is inconclusive because the single-instance run had no successful responses.")
elif latency_delta < 0:
    analysis.append(f"- Average latency improved by {abs(latency_delta):.2f}% with two instances in this environment.")
elif latency_delta > 0:
    if two["success"] == single["success"]:
        analysis.append(f"- Average latency for successful responses was {latency_delta:.2f}% higher with two instances in this local run. Since both scenarios completed all requests, this suggests gateway/discovery overhead, warm-up effects, or local-machine contention outweighed any scaling benefit here.")
    else:
        analysis.append(f"- Average latency for successful responses was {latency_delta:.2f}% higher with two instances. Because the scenarios had different success rates, this should be read together with the failure difference, not in isolation.")
else:
    analysis.append("- Average latency was effectively unchanged between the two scenarios.")

lines.extend([
    "## Analysis",
    "",
    *analysis,
    "",
    "## Notes",
    "",
    "- `Avg latency*` is calculated only from successful (`200`) responses.",
    "- These measurements are environment-dependent; rerunning on another machine or under a different DB/cache warm-up state can change the absolute numbers.",
    "- The adjacent per-scenario CSV files contain the raw per-request measurements if you want to build charts or compute extra statistics.",
])

Path(out_md).write_text("\n".join(lines) + "\n", encoding="utf-8")
PY

echo ""
echo "✓ Comparison report written to $RESULTS_DIR/comparison.md"
