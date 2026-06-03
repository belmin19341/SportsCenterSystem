#!/bin/bash
# Helper: clear user DB tables, restart user-service so DataLoader reseeds with real BCrypt hashes
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

USER_CTR=$(docker ps -q -f name=sportcenter-user-db)
echo "User MySQL container: $USER_CTR"

docker exec -i "$USER_CTR" mysql -u"${DB_USER:-sportcenter}" -p"${DB_PASSWORD:-sportcenter123}" "${USER_DB_NAME:-sportcenter_user_db}" \
  -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE user_achievement; TRUNCATE user_loyalty; TRUNCATE achievement; TRUNCATE users; SET FOREIGN_KEY_CHECKS=1;" \
  2>&1 | grep -v Warning || true

echo "Tables cleared."

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    pids=$(wmic process where "CommandLine like '%user-service-0.0.1-SNAPSHOT.jar%'" get ProcessId 2>/dev/null | grep -E '^[0-9]' | tr -d ' ')
    for pid in $pids; do taskkill //F //PID "$pid" >/dev/null 2>&1 || true; done
else
    pkill -9 -f 'user-service-0.0.1-SNAPSHOT.jar' 2>/dev/null || true
fi
sleep 2

set -a
. ./.env
set +a

nohup java -jar "User Service/target/user-service-0.0.1-SNAPSHOT.jar" > /tmp/user-service.log 2>&1 &
echo "user-service started PID $!"

for i in $(seq 1 60); do
    if curl -sf http://localhost:8081/actuator/health >/dev/null 2>&1; then
        echo "user-service UP after ${i}s"; break
    fi
    sleep 1
done

sleep 2
echo "── Users in DB after reseed ──"
docker exec -i "$USER_CTR" mysql -u"${DB_USER:-sportcenter}" -p"${DB_PASSWORD:-sportcenter123}" "${USER_DB_NAME:-sportcenter_user_db}" \
  -e "SELECT id, username, role, LEFT(password_hash, 12) AS hash_prefix FROM users;" \
  2>&1 | grep -v Warning
