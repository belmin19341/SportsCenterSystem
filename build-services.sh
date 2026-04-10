#!/bin/bash

set -euo pipefail

services=(
    "User Service"
    "Resource Service"
    "Booking Service"
    "Payment Service"
)

echo "═══════════════════════════════════════════════════════════════"
echo "  SportsCenterSystem — Build svih servisa"
echo "═══════════════════════════════════════════════════════════════"
echo ""

for service in "${services[@]}"; do
    echo "▶ Building $service..."
    chmod +x "$service/mvnw"
    (
        cd "$service"
        ./mvnw clean package -DskipTests
    )
    echo "✓ $service build završen"
    echo ""
done

echo "═══════════════════════════════════════════════════════════════"
echo "✓ Svi servisi su uspješno buildani"
echo "═══════════════════════════════════════════════════════════════"