#!/bin/bash

# ═══════════════════════════════════════════════════════════════════
# SportsCenterSystem — Skripta za pokretanje svih servisa
# ═══════════════════════════════════════════════════════════════════

# Load .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '#' | xargs)
    echo "✓ .env konfiguracija učitana"
else
    echo "⚠️  .env fajl nije pronađen. Kopirajte .env.example u .env"
    exit 1
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  SportsCenterSystem — Pokretanje svih servisa"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Konfiguracija:"
echo "  User Service Port:     $USER_SERVICE_PORT"
echo "  Resource Service Port: $RESOURCE_SERVICE_PORT"
echo "  Booking Service Port:  $BOOKING_SERVICE_PORT"
echo "  Payment Service Port:  $PAYMENT_SERVICE_PORT"
echo ""
echo "  Database User:         $DB_USER"
echo "  Database Password:     (skriveno)"
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Check Docker
echo "🐳 Provjera Docker kontejnera..."
docker compose ps > /dev/null 2>&1 || {
    echo "⚠️  Docker kontejneri nisu pokrenuti. Pokretam..."
    docker compose up -d
    sleep 15
}

echo ""
echo "🚀 Pokretanje servisa..."
echo ""

# Check if jars exist
for service in "User Service" "Resource Service" "Booking Service" "Payment Service"; do
    jar_path="${service}/target/${service//Service/}-service-0.0.1-SNAPSHOT.jar"
    jar_path=$(echo "$jar_path" | sed 's/-service-service/-service/g' | sed 's/ //g')

    if [ ! -f "$jar_path" ]; then
        echo "❌ JAR ne postoji za $service"
        exit 1
    fi
done

# Start services
echo "1️⃣  User Service (8081)..."
nohup java -jar "User Service/target/user-service-0.0.1-SNAPSHOT.jar" > /tmp/user-service.log 2>&1 &
echo "   PID: $!"

echo ""
echo "2️⃣  Resource Service (8082)..."
nohup java -jar "Resource Service/target/resource-service-0.0.1-SNAPSHOT.jar" > /tmp/resource-service.log 2>&1 &
echo "   PID: $!"

echo ""
echo "3️⃣  Booking Service (8083)..."
nohup java -jar "Booking Service/target/booking-service-0.0.1-SNAPSHOT.jar" > /tmp/booking-service.log 2>&1 &
echo "   PID: $!"

echo ""
echo "4️⃣  Payment Service (8084)..."
nohup java -jar "Payment Service/target/payment-service-0.0.1-SNAPSHOT.jar" > /tmp/payment-service.log 2>&1 &
echo "   PID: $!"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "✓ Svi servisi su pokrenuti!"
echo ""
echo "Provjera:"
echo "  curl http://localhost:$USER_SERVICE_PORT"
echo "  curl http://localhost:$RESOURCE_SERVICE_PORT"
echo "  curl http://localhost:$BOOKING_SERVICE_PORT"
echo "  curl http://localhost:$PAYMENT_SERVICE_PORT"
echo ""
echo "Logovi:"
echo "  tail -f /tmp/user-service.log"
echo "  tail -f /tmp/resource-service.log"
echo "  tail -f /tmp/booking-service.log"
echo "  tail -f /tmp/payment-service.log"
echo ""
echo "═══════════════════════════════════════════════════════════════"

