package ba.nwt.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT (JSON Web Token) Utility Component — RS256 (asymmetric).
 *
 * <p><b>Signing algorithm:</b> RS256 (RSASSA-PKCS1-v1_5 with SHA-256).
 *
 * <p><b>Trust boundary:</b>
 * <ul>
 *   <li>The RSA <b>private key</b> lives ONLY inside the User Service —
 *       only this service can mint (sign) tokens.</li>
 *   <li>The RSA <b>public key</b> is distributed to verifiers (currently the
 *       API Gateway). Verifiers can validate signatures but cannot forge new
 *       tokens. This eliminates the HS256 shared-secret problem where any
 *       service holding the key was also implicitly a token issuer.</li>
 * </ul>
 *
 * <p><b>Issued claims (registered + custom):</b>
 * <pre>
 *   sub      → user id (registered, identifies the principal)
 *   iss      → "sports-center-user-service" (registered)
 *   aud      → "sports-center-api"          (registered, intended recipient)
 *   iat      → issued-at  (registered)
 *   exp      → expires-at (registered, validated by parser)
 *   jti      → unique id  (registered, used for revocation / blacklist)
 *   type     → "access" | "refresh" (prevents refresh-as-access misuse)
 *   roles    → ["USER"] / ["ADMIN"] / ["OWNER"]
 *   username → cached identifier for downstream logging
 *   email    → cached identifier (access tokens only)
 * </pre>
 *
 * <p><b>Refresh tokens:</b> generated with {@code type=refresh}, only accepted
 * by {@code POST /api/auth/refresh}. Verifiers refuse any token whose
 * {@code type} is not {@code access} on protected routes, so a stolen refresh
 * token cannot be used to call business APIs.
 */
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TYPE   = "type";
    public static final String CLAIM_ROLES  = "roles";
    public static final String TYPE_ACCESS  = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.private-key-path:classpath:keys/jwt-private.pem}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path:classpath:keys/jwt-public.pem}")
    private String publicKeyPath;

    @Value("${jwt.issuer:sports-center-user-service}")
    private String issuer;

    @Value("${jwt.audience:sports-center-api}")
    private String audience;

    @Value("${jwt.kid:sports-key-1}")
    private String keyId;

    @Value("${jwt.access.expiration:${jwt.expiration:900}}")
    private long accessTokenExpirationSec;

    @Value("${jwt.refresh.expiration:604800}")
    private long refreshTokenExpirationSec;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @PostConstruct
    void init() {
        this.privateKey = PemKeyLoader.loadPrivate(privateKeyPath);
        this.publicKey  = PemKeyLoader.loadPublic(publicKeyPath);
    }

    /* ────────────────────────────────────────────────────────────────────── *
     *  Token generation
     * ────────────────────────────────────────────────────────────────────── */

    /**
     * Generate a short-lived ACCESS token used as Authorization: Bearer …
     */
    public String generateAccessToken(Long userId, String username, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("email", email);
        claims.put(CLAIM_ROLES, List.of(role));
        claims.put(CLAIM_TYPE, TYPE_ACCESS);
        return createToken(claims, userId.toString(), accessTokenExpirationSec);
    }

    /**
     * Generate a long-lived REFRESH token. Used only by POST /api/auth/refresh
     * to mint new access tokens. Cannot be used to access protected resources
     * (the gateway rejects tokens with type=refresh).
     */
    public String generateRefreshToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put(CLAIM_ROLES, List.of(role));
        claims.put(CLAIM_TYPE, TYPE_REFRESH);
        return createToken(claims, userId.toString(), refreshTokenExpirationSec);
    }

    /** Backwards-compatible alias used in older code paths. */
    public String generateToken(Long userId, String username, String email, String role) {
        return generateAccessToken(userId, username, email, role);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationSec) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSec * 1000);
        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setHeaderParam("typ", "JWT")
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .setId(UUID.randomUUID().toString())   // jti
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /* ────────────────────────────────────────────────────────────────────── *
     *  Claim extraction / validation
     * ────────────────────────────────────────────────────────────────────── */

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long   getUserIdFromToken(String token)   { return Long.parseLong(getAllClaimsFromToken(token).getSubject()); }
    public String getUsernameFromToken(String token) { return getAllClaimsFromToken(token).get("username", String.class); }

    public String getRoleFromToken(String token) {
        Object roles = getAllClaimsFromToken(token).get(CLAIM_ROLES);
        if (roles instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        return null;
    }

    public String getTypeFromToken(String token) {
        Object t = getAllClaimsFromToken(token).get(CLAIM_TYPE);
        return t == null ? null : t.toString();
    }

    public String getJtiFromToken(String token)        { return getAllClaimsFromToken(token).getId(); }
    public Date   getExpirationDateFromToken(String t) { return getAllClaimsFromToken(t).getExpiration(); }

    public Boolean isTokenExpired(String token) {
        try { return getExpirationDateFromToken(token).before(new Date()); }
        catch (Exception e) { return true; }
    }

    public Boolean validateToken(String token) {
        try { getAllClaimsFromToken(token); return true; }
        catch (Exception e) { return false; }
    }

    public long getAccessTokenExpirationSec()  { return accessTokenExpirationSec; }
    public long getRefreshTokenExpirationSec() { return refreshTokenExpirationSec; }
}
