#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# SportsCenterSystem — End-to-End Security Test Script
#
# Pokreće sve scenarije sigurnosti kroz API Gateway (port 8080):
#   1) Login OK             → 200 + access + refresh
#   2) Login FAIL           → 401 (uniformna poruka)
#   3) Validate token       → 200 valid=true
#   4) Authorized request   → 200
#   5) RBAC missing token   → 401
#   6) Refresh token        → 200 novi access token
#   7) Refresh-as-access    → 401 (refresh ne smije pristupiti zaštićenim rutama)
#   8) Logout               → 200, token revoked
#   9) Use revoked token    → 401
#  10) Rate limit           → poslije N pokušaja = 429
#
# Pokreni iz korijena projekta:
#   chmod +x scripts/security-test.sh
#   ./scripts/security-test.sh
# ═══════════════════════════════════════════════════════════════════════════════
set -u
GATEWAY="${GATEWAY:-http://localhost:8080}"
USER_NAME="${USER_NAME:-john_doe}"
USER_PASS="${USER_PASS:-password123}"
WRONG_PASS="${WRONG_PASS:-wrong_password}"   # >= 6 chars so it passes @Size validation

PASS=0; FAIL=0
ok()   { echo "  ✓ $*"; PASS=$((PASS+1)); }
fail() { echo "  ✗ $*"; FAIL=$((FAIL+1)); }
hr()   { echo ""; echo "── $* ──────────────────────────────────────────────"; }

# Best-effort JSON extractor (no jq required)
json_get() {
  local key="$1"; local body="$2"
  echo "$body" | sed -n 's/.*"'"$key"'":[ ]*"\([^"]*\)".*/\1/p' | head -1
}

# 1) Login OK
hr "1) Login OK ($USER_NAME)"
RESP=$(curl -s -o /tmp/login.json -w '%{http_code}' -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_NAME\",\"password\":\"$USER_PASS\"}" \
  "$GATEWAY/api/auth/login")
echo "  HTTP $RESP"
head -c 400 /tmp/login.json; echo
[ "$RESP" = "200" ] && ok "login 200" || fail "login expected 200, got $RESP"
ACCESS=$(json_get access_token "$(cat /tmp/login.json)")
REFRESH=$(json_get refresh_token "$(cat /tmp/login.json)")
[ -n "$ACCESS" ] && ok "access_token present" || fail "access_token missing"
[ -n "$REFRESH" ] && ok "refresh_token present" || fail "refresh_token missing"

# 2) Login FAIL — wrong password (>=6 chars so validation passes)
hr "2) Login FAIL (wrong password)"
RESP=$(curl -s -o /tmp/login_fail.json -w '%{http_code}' -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_NAME\",\"password\":\"$WRONG_PASS\"}" \
  "$GATEWAY/api/auth/login")
echo "  HTTP $RESP"; cat /tmp/login_fail.json; echo
[ "$RESP" = "401" ] && ok "login fail 401" || fail "expected 401, got $RESP"

# 3) Validate token
hr "3) Validate access token"
RESP=$(curl -s -o /tmp/validate.json -w '%{http_code}' -X POST \
  -H "Authorization: Bearer $ACCESS" "$GATEWAY/api/auth/validate")
echo "  HTTP $RESP"; cat /tmp/validate.json; echo
[ "$RESP" = "200" ] && ok "validate 200" || fail "expected 200, got $RESP"

# 4) Authorized request
hr "4) Authorized GET /api/users"
RESP=$(curl -s -o /tmp/users.json -w '%{http_code}' \
  -H "Authorization: Bearer $ACCESS" "$GATEWAY/api/users")
echo "  HTTP $RESP"; head -c 200 /tmp/users.json; echo
[ "$RESP" = "200" ] && ok "users 200" || fail "expected 200, got $RESP"

# 5) RBAC denial — request without token must be 401
hr "5) RBAC — request without token must be 401"
RESP=$(curl -s -o /tmp/no_token.json -w '%{http_code}' "$GATEWAY/api/users")
echo "  HTTP $RESP"; cat /tmp/no_token.json; echo
[ "$RESP" = "401" ] && ok "no token 401" || fail "expected 401, got $RESP"

# 6) Refresh token
hr "6) Refresh access token"
RESP=$(curl -s -o /tmp/refresh.json -w '%{http_code}' -H 'Content-Type: application/json' \
  -d "{\"refresh_token\":\"$REFRESH\"}" "$GATEWAY/api/auth/refresh")
echo "  HTTP $RESP"; head -c 400 /tmp/refresh.json; echo
[ "$RESP" = "200" ] && ok "refresh 200" || fail "expected 200, got $RESP"
NEW_ACCESS=$(json_get access_token "$(cat /tmp/refresh.json)")
NEW_REFRESH=$(json_get refresh_token "$(cat /tmp/refresh.json)")

# 7) Refresh-as-access — rejected
hr "7) Use refresh token as access (must be 401)"
RESP=$(curl -s -o /tmp/refresh_as_access.json -w '%{http_code}' \
  -H "Authorization: Bearer $REFRESH" "$GATEWAY/api/users")
echo "  HTTP $RESP"; cat /tmp/refresh_as_access.json; echo
[ "$RESP" = "401" ] && ok "refresh-as-access 401" || fail "expected 401, got $RESP"

# 8) Logout
hr "8) Logout"
RESP=$(curl -s -o /tmp/logout.json -w '%{http_code}' -X POST \
  -H "Authorization: Bearer $NEW_ACCESS" -H 'Content-Type: application/json' \
  -d "{\"refresh_token\":\"$NEW_REFRESH\"}" \
  "$GATEWAY/api/auth/logout")
echo "  HTTP $RESP"; cat /tmp/logout.json; echo
[ "$RESP" = "200" ] && ok "logout 200" || fail "expected 200, got $RESP"
sleep 1

# 9) Reuse revoked access token
hr "9) Reuse revoked access token (must be 401)"
RESP=$(curl -s -o /tmp/reuse.json -w '%{http_code}' \
  -H "Authorization: Bearer $NEW_ACCESS" "$GATEWAY/api/users")
echo "  HTTP $RESP"; cat /tmp/reuse.json; echo
[ "$RESP" = "401" ] && ok "revoked-token 401" || fail "expected 401, got $RESP"

# 10) Rate limit
hr "10) Rate limit on login (8 brzih pokušaja sa pogrešnom lozinkom)"
LAST_CODE=""
for i in 1 2 3 4 5 6 7 8; do
  CODE=$(curl -s -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
    -d "{\"username\":\"$USER_NAME\",\"password\":\"$WRONG_PASS\"}" \
    "$GATEWAY/api/auth/login")
  echo "  attempt $i → HTTP $CODE"
  LAST_CODE=$CODE
done
[ "$LAST_CODE" = "429" ] && ok "rate-limited (429)" || fail "expected 429 on last attempt, got $LAST_CODE"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  PASSED: $PASS    FAILED: $FAIL"
echo "═══════════════════════════════════════════════════════════════"
exit $FAIL

