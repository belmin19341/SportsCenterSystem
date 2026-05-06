#!/bin/bash
# Helper: clear user DB tables, restart user-service so DataLoader reseeds with real BCrypt hashes
set -e
USER_CTR=$(docker ps -q -f name=user)
echo "User MySQL container: $USER_CTR"

docker exec -i "$USER_CTR" mysql -usportcenter -psportcenter123 sportcenter_user_db -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE user_achievement; TRUNCATE user_loyalty; TRUNCATE achievement; TRUNCATE users; SET FOREIGN_KEY_CHECKS=1;" 2>&1 | grep -v Warning || true

echo "Tables cleared."

pkill -9 -f 'user-service-0.0.1-SNAPSHOT.jar' 2>/dev/null || true
sleep 2

cd /Users/belmindurmo/IdeaProjects/SportsCenterSystem
# load env
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
docker exec -i "$USER_CTR" mysql -usportcenter -psportcenter123 sportcenter_user_db -e "SELECT id, username, role, LEFT(password_hash, 12) AS hash_prefix FROM users;" 2>&1 | grep -v Warning

