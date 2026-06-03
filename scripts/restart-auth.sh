#!/bin/bash
# Helper: rebuild + restart user-service & api-gateway, wait for health
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "── building user-service ──"
(cd "User Service" && ./mvnw -q -DskipTests package)

echo "── building api-gateway ──"
(cd "API Gateway" && ./mvnw -q -DskipTests package)

echo "── killing old processes ──"
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    # Windows — use taskkill
    for pat in "user-service-0.0.1-SNAPSHOT.jar" "demo-0.0.1-SNAPSHOT.jar"; do
        pids=$(wmic process where "CommandLine like '%$pat%'" get ProcessId 2>/dev/null | grep -E '^[0-9]' | tr -d ' ')
        for pid in $pids; do taskkill //F //PID "$pid" >/dev/null 2>&1 || true; done
    done
else
    pkill -9 -f 'user-service-0.0.1-SNAPSHOT.jar' 2>/dev/null || true
    pkill -9 -f 'demo-0.0.1-SNAPSHOT.jar' 2>/dev/null || true
fi
sleep 3

set -a
. ./.env
set +a

nohup java -jar "User Service/target/user-service-0.0.1-SNAPSHOT.jar" > /tmp/user-service.log 2>&1 &
echo "user-service PID $!"
nohup java -jar "API Gateway/target/demo-0.0.1-SNAPSHOT.jar" > /tmp/api-gateway.log 2>&1 &
echo "api-gateway PID $!"

for i in $(seq 1 60); do
  if curl -sf http://localhost:8081/actuator/health >/dev/null && curl -sf http://localhost:8080/actuator/health >/dev/null; then
    echo "both UP after ${i}s"; break
  fi
  sleep 1
done

echo "── sanity ──"
curl -s -o /dev/null -w 'user-service /actuator/health: %{http_code}\n' http://localhost:8081/actuator/health
curl -s -o /dev/null -w 'api-gateway /actuator/health: %{http_code}\n' http://localhost:8080/actuator/health
