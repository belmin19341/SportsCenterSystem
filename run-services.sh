#!/bin/bash
set -e

# ═══════════════════════════════════════════════════════════════════
# SportsCenterSystem — Pokretanje cijelog stack-a
#   1) Config Server (8888)
#   2) Eureka Discovery (8761)
#   3) 4 mikroservisa (User/Resource/Booking/Payment)
#   4) (opcionalno) druga instanca Resource Service-a za LB demo
# ═══════════════════════════════════════════════════════════════════

if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
    echo "✓ .env učitan"
else
    echo "⚠️  .env fajl nije pronađen. Kopirajte .env.example u .env."
    exit 1
fi

CONFIG_PORT=${CONFIG_SERVER_PORT:-8888}
EUREKA_PORT=${DISCOVERY_SERVER_PORT:-8761}

wait_for_health() {
    local url=$1; local name=$2; local tries=60
    echo -n "   čekam $name na $url ..."
    for i in $(seq 1 $tries); do
        if curl -sf "$url" >/dev/null 2>&1; then echo " UP"; return 0; fi
        sleep 1; echo -n "."
    done
    echo " ❌ timeout"; return 1
}

start_jar() {
    local label=$1; local jar=$2; local logfile=$3; shift 3
    [ -f "$jar" ] || { echo "❌ JAR ne postoji: $jar"; exit 1; }
    nohup java -jar "$jar" "$@" > "$logfile" 2>&1 &
    echo "   $label PID: $!"
}

echo ""
echo "🐳 Provjera Docker kontejnera..."
docker compose ps >/dev/null 2>&1 || { docker compose up -d; sleep 10; }

echo ""
echo "1️⃣  Config Server ($CONFIG_PORT)..."
start_jar "config-server" "config-server/target/config-server-0.0.1-SNAPSHOT.jar" "/tmp/config-server.log"
wait_for_health "http://localhost:$CONFIG_PORT/actuator/health" "config-server"

echo ""
echo "2️⃣  Discovery Server ($EUREKA_PORT)..."
start_jar "discovery-server" "discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar" "/tmp/discovery-server.log"
wait_for_health "http://localhost:$EUREKA_PORT/actuator/health" "discovery-server"

echo ""
echo "3️⃣  Mikroservisi..."
start_jar "user-service     ($USER_SERVICE_PORT)"      "User Service/target/user-service-0.0.1-SNAPSHOT.jar"          "/tmp/user-service.log"
start_jar "resource-service ($RESOURCE_SERVICE_PORT)"  "Resource Service/target/resource-service-0.0.1-SNAPSHOT.jar"  "/tmp/resource-service.log"
start_jar "booking-service  ($BOOKING_SERVICE_PORT)"   "Booking Service/target/booking-service-0.0.1-SNAPSHOT.jar"    "/tmp/booking-service.log"
start_jar "payment-service  ($PAYMENT_SERVICE_PORT)"   "Payment Service/target/payment-service-0.0.1-SNAPSHOT.jar"    "/tmp/payment-service.log"

if [ "$1" = "--lb" ]; then
    LB_PORT=${RESOURCE_SERVICE_PORT_2:-8092}
    echo ""
    echo "4️⃣  Druga instanca Resource Service-a ($LB_PORT) za LB demo..."
    SERVER_PORT=$LB_PORT \
    nohup java -jar "Resource Service/target/resource-service-0.0.1-SNAPSHOT.jar" \
        --server.port=$LB_PORT > /tmp/resource-service-2.log 2>&1 &
    echo "   PID: $!"
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "✓ Sve pokrenuto."
echo "  Eureka:    http://localhost:$EUREKA_PORT"
echo "  Config:    http://localhost:$CONFIG_PORT/actuator/health"
echo "  Swagger:   http://localhost:808X/swagger-ui.html"
echo "  Logovi:    tail -f /tmp/<service>.log"
echo "═══════════════════════════════════════════════════════════════"
