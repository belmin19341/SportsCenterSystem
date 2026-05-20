#!/usr/bin/env bash
#
# Regenerate the RSA keypair used for signing JWT access/refresh tokens.
#
#   - Private key (PKCS#8, 2048-bit RSA)  → User Service ONLY
#   - Public key                          → User Service + API Gateway
#
# This is a one-off helper for the academic project. In production the private
# key would be loaded from a secret manager (Vault / AWS Secrets Manager /
# Kubernetes Secret) and never committed to the repository.
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
USR_KEYS="$ROOT/User Service/src/main/resources/keys"
GW_KEYS="$ROOT/API Gateway/src/main/resources/keys"

mkdir -p "$USR_KEYS" "$GW_KEYS"

TMP_PRIV="$USR_KEYS/jwt-private.tmp.pem"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMP_PRIV" >/dev/null 2>&1
openssl pkcs8 -topk8 -nocrypt -in "$TMP_PRIV" -out "$USR_KEYS/jwt-private.pem" >/dev/null 2>&1
rm "$TMP_PRIV"
openssl rsa -in "$USR_KEYS/jwt-private.pem" -pubout -out "$USR_KEYS/jwt-public.pem" >/dev/null 2>&1
cp "$USR_KEYS/jwt-public.pem" "$GW_KEYS/jwt-public.pem"

echo "Generated:"
echo "  $USR_KEYS/jwt-private.pem (private, User Service only)"
echo "  $USR_KEYS/jwt-public.pem  (public, used locally for refresh validation)"
echo "  $GW_KEYS/jwt-public.pem   (public, API Gateway validation)"

