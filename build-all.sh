#!/bin/bash
set -e

BUILD_FRONTEND=true
SKIP_TESTS=true

for arg in "$@"; do
    case "$arg" in
        --skip-frontend)
            BUILD_FRONTEND=false
            ;;
        --with-tests)
            SKIP_TESTS=false
            ;;
        *)
            echo "❌ Nepoznata opcija: $arg"
            echo "   Upotreba: bash build-all.sh [--skip-frontend] [--with-tests]"
            exit 1
            ;;
    esac
done

if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
    echo "✓ .env učitan"
fi

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "❌ Nedostaje komanda: $1"
        exit 1
    }
}

build_maven_module() {
    local directory=$1
    local label=$2

    echo ""
    echo "🔨 $label..."
    (
        cd "$directory"
        if [ "$SKIP_TESTS" = true ]; then
            sh ./mvnw clean package -DskipTests
        else
            sh ./mvnw clean package
        fi
    )
}

require_command java

build_maven_module "config-server" "Config Server"
build_maven_module "discovery-server" "Discovery Server"
build_maven_module "API Gateway" "API Gateway"
build_maven_module "User Service" "User Service"
build_maven_module "Resource Service" "Resource Service"
build_maven_module "Booking Service" "Booking Service"
build_maven_module "Payment Service" "Payment Service"

if [ "$BUILD_FRONTEND" = true ]; then
    require_command pnpm

    echo ""
    echo "📦 Frontend dependencies..."
    (
        cd frontend
        pnpm install
    )

    echo ""
    echo "🧱 Frontend build..."
    (
        cd frontend
        pnpm build
    )
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "✓ Build završen."
echo "  Za pokretanje cijelog stack-a: bash run-services.sh"
if [ "$BUILD_FRONTEND" = false ]; then
    echo "  Frontend build je preskočen (--skip-frontend)."
fi
echo "═══════════════════════════════════════════════════════════════"
