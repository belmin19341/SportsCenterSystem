# API Authentication & Security Implementation Guide

## Quick Start

### 1. User Login

**Endpoint:** `POST /api/auth/login`

**Request:**
```json
{
  "username": "john_doe",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER",
  "expires_in": 86400
}
```

### 2. Use Token for Subsequent Requests

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 3. Validate Token

**Endpoint:** `POST /api/auth/validate`

**Request:**
```
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
  "valid": true,
  "message": "Token is valid"
}
```

---

## Role-Based Access Control (RBAC)

### USER Role
- Can view profile
- Can create own bookings
- Can view own payments
- Can view available resources

**Permitted Paths:**
- `GET /api/users/profile`
- `GET /api/users/me`
- `GET /api/bookings/my-bookings`
- `POST /api/bookings/create`
- `GET /api/resources/list`
- `GET /api/payments/my-payments`

### OWNER Role
- Full facility management
- Can view all bookings for owned facilities
- Can manage pricing
- Can view reports

**Permitted Paths:**
- All `/api/users/**`
- All `/api/resources/**`
- All `/api/bookings/**`
- All `/api/payments/**`

### ADMIN Role
- Full system access
- User management
- System configuration
- All analytics

**Permitted Paths:**
- All `/api/**`

---

## Security Implementation Details

### JWT Token Structure

The JWT token contains the following claims:

```json
{
  "sub": "1",                           // User ID
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER",
  "roles": ["USER"],
  "jti": "uuid-unique-id",              // For future revocation
  "iat": 1704067200,                    // Issued at
  "exp": 1704153600                     // Expiration (1 day later)
}
```

### Token Expiration

- **Access Token:** 24 hours (86,400 seconds)
- **Refresh Token:** 7 days (if implemented)

### Password Security

- Passwords are hashed using **BCrypt** with 12 rounds
- Never transmitted in plaintext
- Hashed password verification on login

### Gateway-Level Authentication

The API Gateway (`api-gateway:8080`) validates all incoming tokens:

1. **Signature Verification** — Ensures token hasn't been tampered with
2. **Expiration Check** — Rejects expired tokens
3. **Role-Based Routing** — Blocks unauthorized users from specific routes
4. **Request Enhancement** — Adds user context headers:
   - `X-User-Id`: User's database ID
   - `X-User-Name`: Username
   - `X-User-Role`: User's role

### Public Routes (No Auth Required)

- `POST /api/auth/login` — User login
- `POST /api/auth/validate` — Token validation
- `/api-docs` — API documentation
- `/swagger-ui` — Swagger UI
- `/health` — Health check
- `/actuator` — Metrics

---

## Testing the Security Implementation

### 1. Start All Services

```bash
# Using Docker Compose
docker-compose up -d

# Or start services individually:
# - Discovery Server (port 8761)
# - Config Server (port 8888)
# - User Service (port 8081)
# - Booking Service (port 8083)
# - Payment Service (port 8084)
# - Resource Service (port 8082)
# - API Gateway (port 8080)
```

### 2. Create a Test User

First, create a user with a hashed password. The password hash should be created using BCrypt.

**Example using Spring Boot CLI or Java code:**
```java
new BCryptPasswordEncoder(12).encode("password123");
// Output: $2a$12$...
```

Then insert into the `users` table:

```sql
INSERT INTO users (username, email, password_hash, role, created_at)
VALUES ('john_doe', 'john@example.com', '$2a$12$...', 'USER', NOW());
```

### 3. Login and Get Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"password123"}'
```

**Response:**
```json
{
  "access_token": "eyJhbGc...",
  "token_type": "Bearer",
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER",
  "expires_in": 86400
}
```

### 4. Use Token for API Calls

```bash
# Copy the access_token from response
TOKEN="eyJhbGc..."

# Make authenticated request
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Test Authorization Failures

```bash
# Try to access admin endpoint as USER
curl -X GET http://localhost:8080/api/admin/stats \
  -H "Authorization: Bearer $TOKEN"
# Response: 403 Forbidden - Insufficient permissions
```

### 6. Test Expired Token

```bash
# Wait for token to expire (or modify jwt.expiration to 1 second for testing)
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer $EXPIRED_TOKEN"
# Response: 401 Unauthorized - Invalid or expired token
```

---

## Error Responses

### 401 Unauthorized

```json
{
  "error": "Invalid or expired token",
  "status": 401
}
```

Causes:
- Missing Authorization header
- Invalid token format
- Expired token
- Token signature verification failed

### 403 Forbidden

```json
{
  "error": "Insufficient permissions for this resource",
  "status": 403
}
```

Causes:
- User role doesn't have access to the requested path
- Resource ownership validation failed

### 400 Bad Request

```json
{
  "error": "Invalid request",
  "status": 400
}
```

Causes:
- Missing required fields in login request
- Invalid username or password

---

## Security Best Practices

### For Clients

1. **Store Token Securely**
   - Web: Use HttpOnly cookies (preferred) or sessionStorage
   - Mobile: Use secure device storage (Keychain on iOS, encrypted SharedPreferences on Android)
   - Desktop: Use encrypted local storage

2. **Include Token in Requests**
   - Always use `Authorization: Bearer <token>` header
   - Never include token in URL query parameters

3. **Handle Token Expiration**
   - Implement token refresh flow (if refresh tokens are available)
   - Redirect to login on 401 response
   - Clear local storage on logout

4. **Delete Token on Logout**
   - Remove from local storage
   - Clear cookies
   - Optionally notify server (for token blacklist)

### For Deployment

1. **Secure JWT Secret**
   - Never hardcode secrets in source code
   - Use environment variables for production
   - Rotate secrets periodically

2. **Use HTTPS**
   - Enforce HTTPS in production
   - Set secure cookie flag
   - Configure HSTS headers

3. **Rate Limiting**
   - Implement rate limiting on login endpoint
   - Prevent brute-force attacks
   - Monitor failed login attempts

4. **Audit Logging**
   - Log all authentication events
   - Log authorization failures
   - Monitor for suspicious patterns

5. **Network Security**
   - Run services on private network
   - Use service-to-service TLS (optional)
   - Configure firewall rules

---

## Troubleshooting

### "Missing authorization header" error

**Cause:** Authorization header not included in request

**Solution:**
```bash
# Correct format
curl -H "Authorization: Bearer <token>"

# Wrong - missing "Bearer"
curl -H "Authorization: <token>"

# Wrong - missing header entirely
curl http://localhost:8080/api/users/profile
```

### "Invalid or expired token" error

**Cause:** Token signature invalid or token has expired

**Solution:**
- Verify token format is correct
- Check token expiration time
- Regenerate token by logging in again
- Verify JWT secret is the same across services

### "Insufficient permissions" error

**Cause:** User role doesn't have access to requested endpoint

**Solution:**
- Check user's role in database
- Verify endpoint permissions in `JwtAuthenticationFilter`
- Ensure correct role is embedded in token

### Services not starting

**Cause:** Dependency issues or configuration problems

**Solution:**
- Verify all dependencies are installed: `mvn clean install`
- Check environment variables (JWT_SECRET, DATABASE URLs)
- Verify port availability
- Check logs for specific errors

---

## Configuration Reference

### User Service (`user-service`)

**Port:** 8081

**Environment Variables:**
- `JWT_SECRET` — Secret key for signing JWTs
- `JWT_EXPIRATION` — Token validity in seconds (default: 86400)
- `USER_SERVICE_PORT` — Service port (default: 8081)
- `USER_DB_URL` — Database connection string
- `USER_DB_NAME` — Database name

### API Gateway (`api-gateway`)

**Port:** 8080

**Environment Variables:**
- `JWT_SECRET` — Must match User Service secret
- `JWT_EXPIRATION` — Token validity in seconds
- `API_GATEWAY_PORT` — Gateway port (default: 8080)
- `EUREKA_URL` — Service discovery URL

---

## Additional Resources

- [JWT.io](https://jwt.io) — JWT decoder and information
- [Spring Security Docs](https://spring.io/projects/spring-security)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)

---

**Last Updated:** May 5, 2026  
**Version:** 1.0
