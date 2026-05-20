#!/bin/bash
set -e

CALLED_FROM_STACK=false
FORCE_RESTART=false

for arg in "$@"; do
    case "$arg" in
        --from-run-services)
            CALLED_FROM_STACK=true
            ;;
        --force-restart)
            FORCE_RESTART=true
            ;;
        *)
            echo "❌ Nepoznata opcija: $arg"
            echo "   Upotreba: bash run-frontend.sh [--from-run-services] [--force-restart]"
            exit 1
            ;;
    esac
done

if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

FRONTEND_PORT=${FRONTEND_PORT:-5173}
GATEWAY_PORT=${API_GATEWAY_PORT:-8080}
export VITE_API_BASE_URL=${VITE_API_BASE_URL:-http://localhost:$GATEWAY_PORT}

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "❌ Nedostaje komanda: $1"
        exit 1
    }
}

wait_for_health() {
    local url=$1
    local name=$2
    local tries=60
    echo -n "   čekam $name na $url ..."
    for i in $(seq 1 $tries); do
        if curl -sf "$url" >/dev/null 2>&1; then
            echo " UP"
            return 0
        fi
        sleep 1
        echo -n "."
    done
    echo " ❌ timeout"
    return 1
}

require_command curl
require_command pnpm
require_command lsof

[ -f "frontend/package.json" ] || {
    echo "❌ frontend/package.json nije pronađen."
    exit 1
}

if [ "$FORCE_RESTART" = true ]; then
    frontend_pids=$(lsof -ti tcp:"$FRONTEND_PORT" -sTCP:LISTEN 2>/dev/null | sort -u | tr '\n' ' ')
    if [ -n "$frontend_pids" ]; then
        echo "🛑 Zaustavljam frontend na portu $FRONTEND_PORT..."
        for pid in $frontend_pids; do
            kill "$pid"
            while kill -0 "$pid" 2>/dev/null; do
                sleep 1
            done
            echo "   ugašen PID: $pid"
        done
    fi
elif curl -sf "http://localhost:$FRONTEND_PORT" >/dev/null 2>&1; then
    echo "✓ Frontend već radi na http://localhost:$FRONTEND_PORT"
    exit 0
fi

if [ ! -d "frontend/node_modules" ]; then
    echo "📦 Frontend dependencies nisu pronađene — pokrećem pnpm install..."
    (
        cd frontend
        pnpm install
    )
fi

echo "🌐 Frontend ($FRONTEND_PORT)..."
(
    cd frontend
    nohup pnpm exec vite --host 0.0.0.0 --port "$FRONTEND_PORT" > /tmp/frontend.log 2>&1 &
    echo "   frontend PID: $!"
)

wait_for_health "http://localhost:$FRONTEND_PORT" "frontend"

if [ "$CALLED_FROM_STACK" = false ]; then
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "✓ Frontend pokrenut."
    echo "  Frontend:  http://localhost:$FRONTEND_PORT"
    echo "  API base:  $VITE_API_BASE_URL"
    echo "  Log:       tail -f /tmp/frontend.log"
    echo "═══════════════════════════════════════════════════════════════"
fi
