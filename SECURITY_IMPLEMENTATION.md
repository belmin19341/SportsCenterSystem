# Security Implementation for Sports Center System — Microservices Architecture

## Executive Summary

This document outlines the security architecture and implementation strategy for the Sports Center System microservices application. We have selected a **JWT-based (JSON Web Token) centralized authentication with decentralized authorization** approach, implemented at the API Gateway layer with role-based access control (RBAC).

---

## 1. Research Summary: Security Solutions for Microservices

### 1.1 Identified Security Approaches

#### A. OAuth 2.0 + OpenID Connect
- **Pros:** Industry standard, comprehensive, third-party integration support
- **Cons:** Complex, overkill for internal systems, requires external provider
- **Verdict:** Not selected—unnecessary complexity for this use case

#### B. Service-to-Service TLS (mTLS)
- **Pros:** Secure internal communication, mutual authentication
- **Cons:** Does not address user authentication, significant operational overhead
- **Verdict:** Consider as supplementary measure, not primary solution

#### C. JWT + API Gateway Pattern (Selected)
- **Pros:** 
  - Stateless authentication (scalable)
  - Single entry point validation
  - Reduced token processing across services
  - Flexible role management
  - Mobile-friendly (no session state required)
- **Cons:** Token compromise affects entire system; requires careful key management
- **Verdict:** Best fit for current architecture

#### D. Session-Based Authentication
- **Pros:** Traditional, simple to implement
- **Cons:** Stateful (requires session replication), poor scalability for microservices
- **Verdict:** Not suitable for distributed systems

### 1.2 Selected Solution Justification

We have chosen **JWT-based authentication at the API Gateway with role-based authorization** because:

1. **Alignment with Architecture:** The system already uses Spring Cloud Gateway, making it the natural single entry point
2. **Scalability:** Stateless tokens eliminate session replication problems
3. **Microservices-Friendly:** Reduces per-service authentication logic
4. **Mobile Support:** Stateless tokens work seamlessly with mobile clients
5. **Performance:** Gateway-level validation reduces downstream processing

---

## 2. Architecture Design

### 2.1 Authentication Flow

```
┌─────────────────┐
│     Client      │
│   (Web/Mobile)  │
└────────┬────────┘
         │ 1. Login Request (username, password)
         │
         v
┌──────────────────────────────┐
│     API Gateway (Port 8080)   │
│  - Route to User Service      │
└────────┬─────────────────────┘
         │
         v
┌──────────────────────────────┐
│   User Service (Port 8081)    │
│  - Authenticate user          │
│  - Generate JWT token         │
│  - Return token               │
└────────┬─────────────────────┘
         │ 2. Token Response
         v
┌─────────────────┐
│     Client      │
│  Stores Token   │
└────────┬────────┘
         │ 3. Subsequent Requests
         │    (Include token in Authorization header)
         v
┌──────────────────────────────┐
│     API Gateway (Port 8080)   │
│  - Validate JWT token         │
│  - Check expiration           │
│  - Extract user claims        │
│  - Validate user roles        │
└────────┬─────────────────────┘
         │ Valid Token
         v
   Route to Service
```

### 2.2 Token Structure (JWT)

```
Header.Payload.Signature

Header: {
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "sub": "user123",           // User ID (subject)
  "username": "john_doe",
  "email": "john@example.com",
  "roles": ["USER"],
  "iat": 1704067200,          // Issued at
  "exp": 1704153600,          // Expiration (1 day)
  "jti": "unique-token-id"    // JWT ID (for revocation)
}

Signature: HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

---

## 3. Questions & Answers

### **Q1: Will the API Gateway handle authentication?**

**A:** Yes, but only for initial validation and routing.

- **What API Gateway does:**
  - Validate JWT token signature and expiration
  - Extract user claims from token
  - Enforce basic role-based routing rules
  - Block invalid/expired tokens early
  - Add user context to request headers

- **Why:** Prevents invalid requests from reaching services, reduces downstream processing

### **Q2: Token Type — JWT or alternative?**

**A:** JWT (JSON Web Token) with HMAC-SHA256 signing

**Rationale:**
- Compact and self-contained (contains all necessary claims)
- No server-side token storage needed (stateless)
- Well-supported by Spring Security
- Suitable for microservices and mobile clients

**Token Lifetime:**
- **Access Token:** 1 day (86,400 seconds)
- **Refresh Token:** 7 days (optional, for token rotation)

### **Q3: How will user roles be implemented? Permissions per role?**

**A:** Role-Based Access Control (RBAC) with three predefined roles

**Defined Roles:**

| Role  | Permissions | Services Access |
|-------|-------------|-----------------|
| **USER** | - View resources<br>- Create own bookings<br>- View own payment history | User, Booking, Payment, Resource (read-only) |
| **OWNER** | - Manage facilities<br>- View bookings for own facilities<br>- Manage pricing<br>- View reports | User, Booking, Payment, Resource (full) |
| **ADMIN** | - Full system access<br>- User management<br>- All analytics<br>- System configuration | All services (full) |

**Implementation:**
- Roles stored in User entity (`Role` enum)
- Embedded in JWT token claims
- Validated at Gateway for route-level access
- Enforced in individual services for resource-level access

### **Q4: Centralized vs. Decentralized Authorization?**

**A:** **Hybrid Approach — Centralized at Gateway, Decentralized at Services**

| Aspect | Our Approach | Rationale |
|--------|--------------|-----------|
| **Gateway Level** | Centralized | Enforce route-level policies (fast rejection) |
| **Service Level** | Decentralized | Each service validates resource ownership |
| **Token Claims** | Centralized | All roles embedded in JWT |
| **Business Rules** | Decentralized | Services implement domain-specific logic |

**Advantages:**
- ✅ Gateway prevents unauthorized route access
- ✅ Services maintain autonomy for complex business logic
- ✅ No single point of failure for fine-grained authorization
- ✅ Services can evolve independently
- ✅ Reduced latency (no inter-service authorization checks)

**Disadvantages:**
- ⚠️ Role changes require token refresh
- ⚠️ Services must duplicate role validation
- ⚠️ Potential inconsistency if policies diverge

### **Q5: Inter-Service Authorization — Is it needed?**

**A:** Optional but recommended for high-security requirements

**Current Design:**
- **Internal Communication:** Services can communicate freely within the network
- **Authentication Between Services:** Not required (internal network)
- **Authorization Between Services:** Implement service-to-service token validation if needed

**Rationale:**
- ✅ Reduces complexity (internal network assumed secure)
- ✅ Improves performance (no extra validation hops)
- ✅ Services can be decomposed independently later

**Future Enhancement:** Implement service-to-service JWT validation using shared secrets or digital signatures for higher security.

### **Q6: Logout & Token Validity**

**A:** Tokens remain valid until expiration (no token invalidation)

**Current Implementation:**
- Tokens are stateless (no server-side session)
- No explicit logout at gateway level
- Token valid until its expiration time (1 day)

**Logout Strategy:**
1. Client deletes token from local storage
2. User can request new login on client app

**Why This Works:**
- ✅ Stateless design (no session storage needed)
- ✅ Simple and scalable
- ✅ Mobile-friendly

**For High-Security Operations:**
- User can maintain "last_logout_time" in database
- If token's "iat" (issued-at) < last_logout_time, reject token
- Cost: Requires database query per request

### **Q7: Token Invalidation — Is it necessary?**

**A:** Not required for default use case, but consider for:

**When Invalidation Becomes Critical:**
1. **Compromised Token** — User account hacked
2. **Permission Revocation** — User role changed immediately
3. **Password Change** — Should invalidate all existing tokens
4. **Admin Termination** — Immediate access revocation

**Implementation Options:**

| Option | Complexity | Performance | Use Case |
|--------|-----------|-------------|----------|
| **Token Expiration Only** | Low | Best | Default, low-security |
| **Blacklist (Redis)** | Medium | Good | Real-time revocation needed |
| **JWT with `jti` claim** | Medium | Good | Balance of features & performance |
| **Database Lookup** | High | Poor | Avoid (defeats stateless design) |

**Recommended:** Token expiration with Redis blacklist for critical actions (password change, admin termination)

### **Q8: Token Lifetime & Refresh Tokens**

**A:** 1-day access tokens + 7-day refresh tokens (optional)

**Current Timings:**
```
Access Token:  
├─ Valid for: 1 day (86,400 seconds)
└─ Used for: Every API request

Refresh Token (Optional):
├─ Valid for: 7 days (604,800 seconds)
└─ Used for: Obtaining new access tokens without re-login
```

**Refresh Token Flow (if implemented):**

```
1. Client has expired access token + valid refresh token
2. POST /auth/refresh with refresh token
3. Server validates refresh token
4. Server issues new access token
5. Client continues with new token
```

**Current Decision:** Start without refresh tokens (simplicity), add later if UX requires it.

### **Q9: Mobile Device Access — Is it needed?**

**A:** Yes, JWT is fully compatible with mobile clients

**Mobile Access Strategy:**

| Client Type | Token Storage | Considerations |
|-------------|---------------|-----------------|
| **Web Browser** | localStorage / sessionStorage | Vulnerable to XSS; recommend HttpOnly cookies |
| **Mobile App (iOS)** | Keychain | Secure, device-level encryption |
| **Mobile App (Android)** | SharedPreferences (encrypted) | Use encrypted SharedPreferences |
| **Desktop App** | Secure storage / encrypted file | OS-level encryption |

**Implementation:**
- Same JWT endpoint for all clients
- Clients store token securely per platform
- Include token in `Authorization: Bearer <token>` header

**Mobile-Specific Features:**
- Support for app refresh (token refresh endpoint)
- Biometric authentication integration (mobile framework → login API)
- Push notifications for security events (optional)

---

## 4. Implementation Details

### 4.1 Technology Stack

- **Framework:** Spring Boot 3.2.5 + Spring Cloud 2023.0.3
- **Gateway:** Spring Cloud Gateway (WebFlux)
- **Security:** Spring Security + JWT (jjwt library)
- **Token Management:** io.jsonwebtoken:jjwt
- **Optional Caching:** Redis (for token blacklist)

### 4.2 Implementation Phases

#### Phase 1: User Service Authentication ✓
- Add JWT generation/validation
- Create login endpoint
- Add password encryption (BCrypt)

#### Phase 2: API Gateway Authorization ✓
- Add JWT filter/interceptor
- Route-level authorization
- Error handling

#### Phase 3: Service Integration ✓
- Add authentication to protected routes
- Token validation in services (optional)
- Role-based resource access

#### Phase 4: Testing & Hardening (Recommended Future Work)
- Integration tests
- Security testing (token tampering, expiration, etc.)
- Performance testing with high request volume

---

## 5. Security Best Practices Implemented

### 5.1 Password Security
- Passwords hashed with BCrypt (never stored in plaintext)
- Minimum strength requirements enforced
- Salted hashes (BCrypt includes salt)

### 5.2 Token Security
- HMAC-SHA256 signing (prevents tampering)
- Short expiration time (1 day)
- Token ID (`jti`) claim for future revocation
- Secure key management (application.properties with secrets)

### 5.3 Transmission Security
- HTTPS recommended in production (not HTTP)
- Authorization header instead of query parameters
- CORS configuration to prevent unauthorized origin access

### 5.4 Gateway Security
- Early validation of tokens (fail fast)
- Clear error responses (no sensitive data leaked)
- Request filtering for malicious payloads
- Rate limiting (future enhancement)

### 5.5 API Security
- Endpoint authentication/authorization required
- Resource-level ownership validation
- Audit logging of sensitive operations
- CORS headers properly configured

---

## 6. Deployment Considerations

### 6.1 Configuration Management

**Development:**
```properties
jwt.secret=your-secret-key-development-only
jwt.expiration=86400
jwt.refresh.expiration=604800
```

**Production:**
```properties
jwt.secret=${JWT_SECRET_ENV_VAR}  # From environment variable
jwt.expiration=86400
jwt.refresh.expiration=604800
```

### 6.2 Key Management
- Never hardcode secrets in source code
- Use environment variables for sensitive data
- Rotate keys periodically (annually minimum)
- Different secrets for different environments

### 6.3 Monitoring
- Log authentication failures
- Monitor token validation errors
- Alert on suspicious patterns
- Track performance impact of security checks

---

## 7. Future Enhancements

1. **Refresh Token Rotation:** Auto-rotate refresh tokens
2. **Token Blacklist:** Implement Redis-based token revocation
3. **OAuth 2.0 Integration:** Support third-party authentication
4. **Multi-Factor Authentication (MFA):** TOTP-based 2FA
5. **Service-to-Service Authentication:** mTLS for internal communication
6. **API Key Management:** For programmatic access
7. **Audit Logging:** Comprehensive security event logging
8. **Rate Limiting:** Prevent brute-force attacks

---

## 8. References

- Spring Security Docs: https://spring.io/projects/spring-security
- JWT.io: https://jwt.io
- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- Spring Cloud Gateway: https://spring.io/projects/spring-cloud-gateway

---

**Document Version:** 1.0  
**Last Updated:** May 5, 2026  
**Authors:** Development Team
