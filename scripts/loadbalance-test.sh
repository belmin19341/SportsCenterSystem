#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# Spring Cloud LoadBalancer demo — sends N requests against Booking Service's
# /api/lb-demo/resource-instance endpoint, which fans out to resource-service via
# the load-balanced RestTemplate. Internally calls the non-trivial business endpoint:
#
#   GET /api/pricing-rules/calculate?facilityId=1&start=...&end=...
#
# This endpoint executes a transactional @EntityGraph query on PricingRule,
# applies multi-rule multipliers (time-slot + day-of-week), and returns a
# PriceQuote — demonstrating that real business logic is distributed across
# Resource Service instances, not just a trivial health check.
#
# Each upstream Resource Service instance stamps the response with X-Instance-Id;
# we tally the distribution and latency via the X-Upstream-Instance header
# bubbled back by Booking Service.
#
# Prerequisite: ./run-services.sh --lb  (starts a 2nd Resource Service instance)
# ═══════════════════════════════════════════════════════════════════════════════
set -e

if [ -f .env ]; then export $(grep -v '^#' .env | xargs); fi
BOOKING_PORT=${BOOKING_SERVICE_PORT:-8093}
N=${1:-100}
URL="http://localhost:${BOOKING_PORT}/api/lb-demo/resource-instance"

OUTDIR="$(dirname "$0")"
RESULTS="${OUTDIR}/loadbalance-results.md"

echo "Sending $N requests to $URL ..."
TMP=$(mktemp)
TOTAL_MS=0
SUCCESS=0
for i in $(seq 1 "$N"); do
    START=$(date +%s%3N)
    INSTANCE=$(curl -s -D - "$URL" -o /dev/null | awk -F': ' '/^X-Upstream-Instance:/ {print $2}' | tr -d '\r')
    END=$(date +%s%3N)
    if [ -n "$INSTANCE" ]; then
        echo "$INSTANCE" >> "$TMP"
        TOTAL_MS=$((TOTAL_MS + END - START))
        SUCCESS=$((SUCCESS + 1))
    fi
done

if [ "$SUCCESS" -eq 0 ]; then
    echo "❌ No successful requests. Is the stack running with --lb?"
    exit 1
fi

AVG_MS=$((TOTAL_MS / SUCCESS))

{
    echo "# Spring Cloud LoadBalancer — Distribution Report"
    echo ""
    echo "- Booking Service endpoint: \`$URL\`"
    echo "- Upstream Resource Service endpoint: \`GET /api/pricing-rules/calculate\` (transactional EntityGraph + multi-rule price calc)"
    echo "- Requests sent: $N"
    echo "- Successful responses: $SUCCESS"
    echo "- Average latency: ${AVG_MS} ms"
    echo ""
    echo "## Distribution by upstream instance"
    echo ""
    echo "| Instance | Hits | Share |"
    echo "| --- | ---: | ---: |"
    sort "$TMP" | uniq -c | sort -rn | while read -r COUNT INSTANCE; do
        SHARE=$(awk "BEGIN{printf \"%.1f%%\", ($COUNT/$SUCCESS)*100}")
        echo "| \`$INSTANCE\` | $COUNT | $SHARE |"
    done
} | tee "$RESULTS"

rm -f "$TMP"
echo ""
echo "✓ Results written to $RESULTS"
