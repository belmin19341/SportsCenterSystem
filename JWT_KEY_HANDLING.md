# JWT Key Handling - Filesystem-Based Implementation Guide

## Overview
This document describes the refactored JWT key handling system for the Sports Center microservices platform. JWT keys are now loaded from the filesystem instead of the classpath, ensuring production-safe and reliable operation across all services.

## Architecture

### Key Distribution
```
┌─────────────────────────────────────────────────────────┐
│  RSA Key Pair (2048-bit)                                │
├──────────────────────┬──────────────────────────────────┤
│  PRIVATE KEY         │  PUBLIC KEY                      │
├──────────────────────┼──────────────────────────────────┤
│  Held by:            │  Held by:                        │
│  • User Service      │  • User Service (validation)     │
│  ONLY                │  • API Gateway (verification)    │
└──────────────────────┴──────────────────────────────────┘
```

### Storage Structure
```
./keys/
├── jwt-private.pem        (User Service only)
└── jwt-public.pem         (All services)
```

### Configuration Paths
- **All services use relative paths**: `file:./keys/jwt-*.pem`
- **Can be overridden via environment variables**:
  - `JWT_PRIVATE_KEY_PATH` (User Service)
  - `JWT_PUBLIC_KEY_PATH` (both services)

## File Format

### Private Key (PKCS#8 Format)
```
-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCx...
[base64 encoded content - 64 chars per line]
...
-----END PRIVATE KEY-----
```

### Public Key (X.509 Format)
```
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsx...
[base64 encoded content - 64 chars per line]
...
-----END PUBLIC KEY-----
```

## Key Generation

### Option 1: Using the Provided Script
```bash
# Generate 2048-bit RSA keypair
./scripts/gen-jwt-keys.sh

# Keys will be created in:
# - User Service/src/main/resources/keys/
# - API Gateway/src/main/resources/keys/
# - ./keys/ (for external deployment)
```

### Option 2: Manual Generation with OpenSSL
```bash
# Generate private key (PKCS#8)
openssl genrsa -out jwt-private.pem 2048
openssl pkcs8 -topk8 -inform PEM -outform PEM \
  -in jwt-private.pem -out jwt-private-pkcs8.pem -nocrypt

# Extract public key
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem

# Place both in ./keys/ directory
```

## Service Configuration

### User Service (`user-service-dev.properties`)
```properties
# JWT Configuration - loads from filesystem
jwt.algorithm=RS256
jwt.private-key-path=file:./keys/jwt-private.pem
jwt.public-key-path=file:./keys/jwt-public.pem
jwt.issuer=sports-center-user-service
jwt.audience=sports-center-api
jwt.kid=sports-key-1
jwt.access.expiration=900
jwt.refresh.expiration=604800
```

**Environment Override:**
```bash
# Override with environment variables
export JWT_PRIVATE_KEY_PATH=file:/custom/keys/jwt-private.pem
export JWT_PUBLIC_KEY_PATH=file:/custom/keys/jwt-public.pem
```

### API Gateway (`api-gateway-dev.properties`)
```properties
# JWT Configuration - loads public key only
jwt.algorithm=RS256
jwt.public-key-path=file:./keys/jwt-public.pem
jwt.issuer=sports-center-user-service
jwt.audience=sports-center-api
```

## Startup Validation

Both services include automatic startup validation beans that:
1. ✓ Verify key files exist before initialization
2. ✗ Fail fast with actionable error messages if missing
3. ✓ Log detailed instructions for resolution

### User Service Validation Output
```
================================================================================
STARTUP VALIDATION: Checking JWT key files...
================================================================================
✓ JWT PRIVATE key found at: ./keys/jwt-private.pem
✓ JWT PUBLIC key found at: ./keys/jwt-public.pem
================================================================================
✓ JWT keys validation PASSED. All required key files exist.
================================================================================
```

### Error Example
```
╔════════════════════════════════════════════════════════════════╗
║  FATAL: JWT PRIVATE KEY NOT FOUND                             ║
╠════════════════════════════════════════════════════════════════╣
║  Expected location: ./keys/jwt-private.pem
║
║  Action Required:
║  1. Generate RSA keys using: scripts/gen-jwt-keys.sh
║  2. Copy keys to: ./keys/
║  3. Restart the application
║
║  DO NOT:
║  - Use classpath: prefixes in application.properties
║  - Store keys in src/main/resources
║  - Generate keys at runtime in production
╚════════════════════════════════════════════════════════════════╝
```

## Deployment Scenarios

### Local Development
```bash
# 1. Generate keys
./scripts/gen-jwt-keys.sh

# 2. Keys are placed in ./keys/ automatically
# 3. Start services - they will load keys from ./keys/
```

### Docker Deployment
```dockerfile
# In Dockerfile for User Service
FROM openjdk:17-slim
WORKDIR /app

# Copy application
COPY build/libs/user-service.jar .

# IMPORTANT: Keys must be mounted as volume at runtime
# docker run -v /host/path/keys:/app/keys user-service:latest

ENTRYPOINT ["java", "-jar", "user-service.jar"]
```

```yaml
# In docker-compose.yml
services:
  user-service:
    build: ./User\ Service
    volumes:
      - ./keys:/app/keys:ro  # Mount keys as read-only
    environment:
      JWT_PRIVATE_KEY_PATH: file:./keys/jwt-private.pem
      JWT_PUBLIC_KEY_PATH: file:./keys/jwt-public.pem
    ports:
      - "8081:8081"

  api-gateway:
    build: ./API\ Gateway
    volumes:
      - ./keys:/app/keys:ro  # Mount only public key location
    environment:
      JWT_PUBLIC_KEY_PATH: file:./keys/jwt-public.pem
    ports:
      - "8080:8080"
```

### Kubernetes Deployment
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jwt-keys
type: Opaque
data:
  jwt-private.pem: <base64-encoded-private-key>
  jwt-public.pem: <base64-encoded-public-key>
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  template:
    spec:
      containers:
      - name: user-service
        volumeMounts:
        - name: jwt-keys
          mountPath: /app/keys
          readOnly: true
        env:
        - name: JWT_PRIVATE_KEY_PATH
          value: file:./keys/jwt-private.pem
        - name: JWT_PUBLIC_KEY_PATH
          value: file:./keys/jwt-public.pem
      volumes:
      - name: jwt-keys
        secret:
          secretName: jwt-keys
```

## Code Changes Summary

### User Service - PemKeyLoader.java
**Before:**
- Used `ClassPathResource` and `DefaultResourceLoader`
- Called `ensureKeysExist()` which generated keys at runtime if missing
- Wrote keys to `src/main/resources` during runtime

**After:**
- Uses NIO `Files` API for filesystem access
- Validates keys exist with clear error messages
- No key generation at runtime
- Supports `file:` URI scheme

### API Gateway - PemKeyLoader.java
**Before:**
- Used `DefaultResourceLoader` for classpath resource loading

**After:**
- Uses NIO `Files` API for filesystem access
- Explicit validation and error handling
- No classpath dependency

## Testing

### Unit Tests
Tests now use the same filesystem paths as production:
```properties
# src/test/resources/application-test.properties
jwt.private-key-path=file:./keys/jwt-private.pem
jwt.public-key-path=file:./keys/jwt-public.pem
```

Ensure test keys are available before running tests:
```bash
# Generate keys first (if not already done)
./scripts/gen-jwt-keys.sh

# Run tests
mvn test -pl "User Service" -Dtest=JwtTokenProviderTest
```

### Integration Tests
Keys must be present in `./keys/` for integration tests to pass.

## Troubleshooting

### Error: "FATAL: JWT PRIVATE KEY NOT FOUND"
**Cause:** Key file doesn't exist at configured path  
**Solution:**
```bash
# 1. Verify key file exists
ls -la ./keys/jwt-private.pem

# 2. If missing, generate it
./scripts/gen-jwt-keys.sh

# 3. Verify content is valid
head -n 2 ./keys/jwt-private.pem
# Should show: -----BEGIN PRIVATE KEY-----
```

### Error: "Classpath resource paths are not supported for JWT keys"
**Cause:** Configuration still uses `classpath:` prefix  
**Solution:**
```bash
# Check configuration files
grep -r "classpath:.*jwt" .

# Update to use file: prefix
# Before: jwt.private-key-path=classpath:keys/jwt-private.pem
# After:  jwt.private-key-path=file:./keys/jwt-private.pem
```

### Error: "Cannot load JWT public key"
**Cause:** 
1. File doesn't exist
2. File is corrupted
3. File has wrong format

**Solution:**
```bash
# Check file exists and is readable
file ./keys/jwt-public.pem
# Should output: ASCII text

# Verify PEM format
head -c 27 ./keys/jwt-public.pem
# Should show: -----BEGIN PUBLIC KEY-----

# Check file permissions
ls -la ./keys/jwt-public.pem
# Should be readable (r-----)

# Regenerate if corrupted
./scripts/gen-jwt-keys.sh
```

## Backward Compatibility

- ✓ No changes required to token generation/validation logic
- ✓ Existing tokens remain valid
- ✓ No API changes
- ✓ Transparent to client applications

## Security Considerations

### Key File Permissions
```bash
# Set restrictive permissions
chmod 400 ./keys/jwt-private.pem  # Read-only by owner
chmod 444 ./keys/jwt-public.pem   # Read-only (can be world-readable)

# Verify
ls -la ./keys/
# -r-------- 1 user group jwt-private.pem
# -r--r--r-- 1 user group jwt-public.pem
```

### Key Rotation
1. Generate new keypair: `./scripts/gen-jwt-keys.sh`
2. Update both files in `./keys/`
3. Restart services (they reload keys on init)
4. Existing tokens signed with old key remain valid until expiration
5. New tokens will use new key

### Never
- ❌ Commit keys to version control (use .gitignore)
- ❌ Store keys in Docker images (use volumes/secrets)
- ❌ Share private keys between environments
- ❌ Use same keys for multiple deployments
- ❌ Generate keys at runtime in production

## References
- [RFC 7517 - JSON Web Key (JWK)](https://tools.ietf.org/html/rfc7517)
- [RFC 7518 - JSON Web Algorithms (JWA)](https://tools.ietf.org/html/rfc7518)
- [Spring Security OAuth2 Resource Server Docs](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
