#!/bin/bash
set -e

# ═══════════════════════════════════════════════════════════════════
# SportsCenterSystem — Starting the entire stack
#   1) Config Server (8888)
#   2) Eureka Discovery (8761)
#   3) 4 mikroservisa (User/Resource/Booking/Payment)
#   4) API Gateway (8080)
#   5) Frontend (5173)
#   6) (opcionalno) druga instanca Resource Service-a za LB demo
# ═══════════════════════════════════════════════════════════════════

START_FRONTEND=true
START_LB=false
RESEED_DATA=false

for arg in "$@"; do
    case "$arg" in
        --backend-only)
            START_FRONTEND=false
            ;;
        --lb)
            START_LB=true
            ;;
        --reseed)
            RESEED_DATA=true
            ;;
        *)
            echo "❌ Unknown option: $arg"
            echo "   Usage: bash run-services.sh [--lb] [--backend-only] [--reseed]"
            exit 1
            ;;
    esac
done

if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
    export JPA_HIBERNATE_DDL_AUTO=${JPA_HIBERNATE_DDL_AUTO:-update}
    export JPA_SHOW_SQL=${JPA_SHOW_SQL:-true}
    export JPA_FORMAT_SQL=${JPA_FORMAT_SQL:-true}

    if [ -z "$VITE_API_BASE_URL" ]; then
        echo "❌ VITE_API_BASE_URL is not defined in .env."
        echo "   This variable is mandatory for the frontend to communicate with the services."
        exit 1
    fi

    echo "✓ .env loaded"
else
    echo "⚠️  .env file not found. Please copy .env.example to .env."
    exit 1
fi

CONFIG_PORT=${CONFIG_SERVER_PORT:-8888}
EUREKA_PORT=${DISCOVERY_SERVER_PORT:-8761}
GATEWAY_PORT=${API_GATEWAY_PORT:-8080}
FRONTEND_PORT=${FRONTEND_PORT:-5173}
SPRING_JPA_DDL_ARG="--spring.jpa.hibernate.ddl-auto=${JPA_HIBERNATE_DDL_AUTO}"
SPRING_JPA_SHOW_SQL_ARG="--spring.jpa.show-sql=${JPA_SHOW_SQL}"
SPRING_JPA_FORMAT_SQL_ARG="--spring.jpa.properties.hibernate.format_sql=${JPA_FORMAT_SQL}"
if [ "$RESEED_DATA" = true ]; then
    SPRING_JPA_DDL_ARG="--spring.jpa.hibernate.ddl-auto=create"
fi
CONFIG_HEALTH_URL="http://localhost:$CONFIG_PORT/actuator/health"
DISCOVERY_HEALTH_URL="http://localhost:$EUREKA_PORT/actuator/health"
USER_HEALTH_URL="http://localhost:${USER_SERVICE_PORT:-8081}/actuator/health"
RESOURCE_HEALTH_URL="http://localhost:${RESOURCE_SERVICE_PORT:-8082}/actuator/health"
BOOKING_HEALTH_URL="http://localhost:${BOOKING_SERVICE_PORT:-8083}/actuator/health"
PAYMENT_HEALTH_URL="http://localhost:${PAYMENT_SERVICE_PORT:-8084}/actuator/health"
GATEWAY_HEALTH_URL="http://localhost:$GATEWAY_PORT/actuator/health"

wait_for_health() {
    local url=$1; local name=$2; local tries=120
    echo -n "   waiting for $name at $url ..."
    for i in $(seq 1 $tries); do
        if curl -sf "$url" >/dev/null 2>&1; then echo " UP"; return 0; fi
        sleep 1; echo -n "."
    done
    echo " ❌ timeout"; return 1
}

is_healthy() {
    local url=$1
    curl -sf "$url" >/dev/null 2>&1
}

docker_container_status() {
    local container=$1
    docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || echo "missing"
}

wait_for_container() {
    local container=$1; local name=$2; local tries=${3:-120}
    echo -n "   waiting for $name ..."
    for i in $(seq 1 $tries); do
        local status
        status=$(docker_container_status "$container")
        case "$status" in
            healthy|running)
                echo " $status"
                return 0
                ;;
            missing)
                echo " ❌ container not found"
                return 1
                ;;
        esac
        sleep 1; echo -n "."
    done
    echo " ❌ timeout"
    return 1
}

start_jar() {
    local label=$1; local jar=$2; local logfile=$3; shift 3
    [ -f "$jar" ] || { echo "❌ JAR does not exist: $jar"; exit 1; }
    nohup java -jar "$jar" "$@" > "$logfile" 2>&1 &
    echo "   $label PID: $!"
}

ensure_jar_service() {
    local label=$1; local port=$2; local health_url=$3; local jar=$4; local logfile=$5; shift 5

    if [ "$RESEED_DATA" = true ]; then
        stop_port_processes "$port" "$label"
        start_jar "$label" "$jar" "$logfile" "$@"
        return 0
    fi

    if is_healthy "$health_url"; then
        echo "   $label is already running"
        return 0
    fi

    start_jar "$label" "$jar" "$logfile" "$@"
}

verify_reseed_seed_users() {
    local response_file status_code
    response_file=$(mktemp)
    status_code=$(curl -s -o "$response_file" -w "%{http_code}" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"password123"}' \
        "http://localhost:${USER_SERVICE_PORT:-8081}/api/auth/login")

    if [ "$status_code" != "200" ] || ! grep -q '"access_token"' "$response_file"; then
        echo "❌ Reseed check failed — seed user admin/password123 is not available."
        echo "   User Service response:"
        cat "$response_file"
        rm -f "$response_file"
        exit 1
    fi

    rm -f "$response_file"
    echo "✓ Seed users verified (admin/password123)"
}

ensure_health_or_fail() {
    local url=$1; local name=$2; local logfile=$3
    wait_for_health "$url" "$name" || {
        echo "❌ $name did not become available. Check log: $logfile"
        exit 1
    }
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "❌ Missing command: $1"
        exit 1
    }
}

stop_port_processes() {
    local port=$1
    local label=$2

    local pids=""
    if command -v lsof >/dev/null 2>&1; then
        pids=$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sort -u | tr '\n' ' ')
    elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
        # Windows fallback using netstat to find PID of process listening on the port
        pids=$(netstat -ano | grep ":$port" | grep "LISTENING" | awk '{print $5}' | sort -u | tr '\n' ' ')
    fi

    [ -n "$pids" ] || return 0

    echo "🛑 Stopping $label on port $port..."
    for pid in $pids; do
        kill "$pid"
        while kill -0 "$pid" 2>/dev/null; do
            sleep 1
        done
        echo "   killed PID: $pid"
    done
}

reset_local_state() {
    echo ""
    echo "♻️  Resetting local stack and Docker volumes..."

    stop_port_processes "$FRONTEND_PORT" "frontend"
    stop_port_processes "$GATEWAY_PORT" "api-gateway"
    stop_port_processes "${USER_SERVICE_PORT:-8081}" "user-service"
    stop_port_processes "${RESOURCE_SERVICE_PORT:-8082}" "resource-service"
    stop_port_processes "${BOOKING_SERVICE_PORT:-8083}" "booking-service"
    stop_port_processes "${PAYMENT_SERVICE_PORT:-8084}" "payment-service"
    stop_port_processes "${RESOURCE_SERVICE_PORT_2:-8092}" "resource-service-2"
    stop_port_processes "$EUREKA_PORT" "discovery-server"
    stop_port_processes "$CONFIG_PORT" "config-server"

    docker compose down -v >/dev/null
    rm -f /tmp/config-server.log \
        /tmp/discovery-server.log \
        /tmp/user-service.log \
        /tmp/resource-service.log \
        /tmp/resource-service-2.log \
        /tmp/booking-service.log \
        /tmp/payment-service.log \
        /tmp/api-gateway.log \
        /tmp/frontend.log

    echo "✓ Local data and processes cleared"
}

ensure_backend_artifacts() {
    local artifacts=(
        "config-server/target/config-server-0.0.1-SNAPSHOT.jar"
        "discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar"
        "API Gateway/target/demo-0.0.1-SNAPSHOT.jar"
        "User Service/target/user-service-0.0.1-SNAPSHOT.jar"
        "Resource Service/target/resource-service-0.0.1-SNAPSHOT.jar"
        "Booking Service/target/booking-service-0.0.1-SNAPSHOT.jar"
        "Payment Service/target/payment-service-0.0.1-SNAPSHOT.jar"
    )

    for artifact in "${artifacts[@]}"; do
        if [ ! -f "$artifact" ]; then
            echo ""
            echo "🔨 Missing build artifacts — running bash build-all.sh --skip-frontend"
            bash build-all.sh --skip-frontend
            return
        fi
    done
}

require_command curl
require_command docker
require_command java

if [ "$RESEED_DATA" = true ]; then
    reset_local_state
    echo ""
    echo "🔨 Reseed build — running bash build-all.sh --skip-frontend"
    bash build-all.sh --skip-frontend
else
    ensure_backend_artifacts
fi

echo ""
echo "🐳 Starting/checking Docker infrastructure..."
docker compose up -d >/dev/null
wait_for_container "sportcenter-user-db" "mysql-user"
wait_for_container "sportcenter-resource-db" "mysql-resource"
wait_for_container "sportcenter-booking-db" "mysql-booking"
wait_for_container "sportcenter-payment-db" "mysql-payment"
wait_for_container "sportcenter-rabbitmq" "rabbitmq"

echo ""
echo "1️⃣  Config Server ($CONFIG_PORT)..."
ensure_jar_service "config-server" "$CONFIG_PORT" "$CONFIG_HEALTH_URL" "config-server/target/config-server-0.0.1-SNAPSHOT.jar" "/tmp/config-server.log"
ensure_health_or_fail "$CONFIG_HEALTH_URL" "config-server" "/tmp/config-server.log"

echo ""
echo "2️⃣  Discovery Server ($EUREKA_PORT)..."
ensure_jar_service "discovery-server" "$EUREKA_PORT" "$DISCOVERY_HEALTH_URL" "discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar" "/tmp/discovery-server.log"
ensure_health_or_fail "$DISCOVERY_HEALTH_URL" "discovery-server" "/tmp/discovery-server.log"

echo ""
echo "3️⃣  Microservices..."
ensure_jar_service "user-service     (${USER_SERVICE_PORT:-8081})" "${USER_SERVICE_PORT:-8081}" "$USER_HEALTH_URL" "User Service/target/user-service-0.0.1-SNAPSHOT.jar" "/tmp/user-service.log" "$SPRING_JPA_DDL_ARG" "$SPRING_JPA_SHOW_SQL_ARG" "$SPRING_JPA_FORMAT_SQL_ARG"
ensure_jar_service "resource-service (${RESOURCE_SERVICE_PORT:-8082})" "${RESOURCE_SERVICE_PORT:-8082}" "$RESOURCE_HEALTH_URL" "Resource Service/target/resource-service-0.0.1-SNAPSHOT.jar" "/tmp/resource-service.log" "$SPRING_JPA_DDL_ARG" "$SPRING_JPA_SHOW_SQL_ARG" "$SPRING_JPA_FORMAT_SQL_ARG"
ensure_jar_service "booking-service  (${BOOKING_SERVICE_PORT:-8083})" "${BOOKING_SERVICE_PORT:-8083}" "$BOOKING_HEALTH_URL" "Booking Service/target/booking-service-0.0.1-SNAPSHOT.jar" "/tmp/booking-service.log" "$SPRING_JPA_DDL_ARG" "$SPRING_JPA_SHOW_SQL_ARG" "$SPRING_JPA_FORMAT_SQL_ARG"
ensure_jar_service "payment-service  (${PAYMENT_SERVICE_PORT:-8084})" "${PAYMENT_SERVICE_PORT:-8084}" "$PAYMENT_HEALTH_URL" "Payment Service/target/payment-service-0.0.1-SNAPSHOT.jar" "/tmp/payment-service.log" "$SPRING_JPA_DDL_ARG" "$SPRING_JPA_SHOW_SQL_ARG" "$SPRING_JPA_FORMAT_SQL_ARG"

ensure_health_or_fail "$USER_HEALTH_URL" "user-service" "/tmp/user-service.log"
ensure_health_or_fail "$RESOURCE_HEALTH_URL" "resource-service" "/tmp/resource-service.log"
ensure_health_or_fail "$BOOKING_HEALTH_URL" "booking-service" "/tmp/booking-service.log"
ensure_health_or_fail "$PAYMENT_HEALTH_URL" "payment-service" "/tmp/payment-service.log"

echo ""
echo "4️⃣  API Gateway ($GATEWAY_PORT)..."
ensure_jar_service "api-gateway      ($GATEWAY_PORT)" "$GATEWAY_PORT" "$GATEWAY_HEALTH_URL" "API Gateway/target/demo-0.0.1-SNAPSHOT.jar" "/tmp/api-gateway.log"
ensure_health_or_fail "$GATEWAY_HEALTH_URL" "api-gateway" "/tmp/api-gateway.log"

if [ "$RESEED_DATA" = true ]; then
    echo ""
    echo "5️⃣  Seed user verification..."
    verify_reseed_seed_users
fi

if [ "$START_FRONTEND" = true ]; then
    echo ""
    if [ "$RESEED_DATA" = true ]; then
        bash run-frontend.sh --from-run-services --force-restart
    else
        bash run-frontend.sh --from-run-services
    fi
fi

if [ "$START_LB" = true ]; then
    LB_PORT=${RESOURCE_SERVICE_PORT_2:-8092}
    LB_HEALTH_URL="http://localhost:$LB_PORT/actuator/health"
    echo ""
    if is_healthy "$LB_HEALTH_URL"; then
        echo "🧪 Second instance of Resource Service ($LB_PORT) is already running"
    else
        echo "🧪 Second instance of Resource Service ($LB_PORT) for LB demo..."
        SERVER_PORT=$LB_PORT \
        nohup java -jar "Resource Service/target/resource-service-0.0.1-SNAPSHOT.jar" \
            --server.port=$LB_PORT > /tmp/resource-service-2.log 2>&1 &
        echo "   PID: $!"
        ensure_health_or_fail "$LB_HEALTH_URL" "resource-service-2" "/tmp/resource-service-2.log"
    fi
else
    LB_PORT=${RESOURCE_SERVICE_PORT_2:-8092}
    if lsof -ti tcp:"$LB_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
        echo ""
        echo "🧪 Stopping resource-service-2 ($LB_PORT) for single-instance mode..."
        stop_port_processes "$LB_PORT" "resource-service-2"
    fi
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "✓ Everything started."
echo "  Eureka:    http://localhost:$EUREKA_PORT"
echo "  Config:    http://localhost:$CONFIG_PORT/actuator/health"
echo "  Gateway:   http://localhost:$GATEWAY_PORT/actuator/health"
if [ "$START_FRONTEND" = true ]; then
    echo "  Frontend:  http://localhost:$FRONTEND_PORT"
else
    echo "  Frontend:  skipped (--backend-only)"
fi
echo "  Swagger:   http://localhost:808X/swagger-ui.html"
echo "  Logs:      tail -f /tmp/<service>.log"
if [ "$START_LB" = true ]; then
    echo "  LB mode:   2x resource-service (${RESOURCE_SERVICE_PORT:-8082}, ${RESOURCE_SERVICE_PORT_2:-8092})"
else
    echo "  LB mode:   1x resource-service (${RESOURCE_SERVICE_PORT:-8082})"
fi
echo "═══════════════════════════════════════════════════════════════"
