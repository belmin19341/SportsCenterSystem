package ba.nwt.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT (JSON Web Token) Utility Component.
 *
 * Generates two distinct token types:
 *   - access  (short-lived, default 15 min) → carried in Authorization: Bearer …
 *   - refresh (long-lived,  default  7 days) → only valid on POST /api/auth/refresh
 *
 * Both tokens carry a unique {@code jti} so they can be revoked individually
 * via the gateway blacklist (access) or the local refresh blacklist (refresh).
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:SportsCenterSystemSecretKeyForJWTTokenDevelopmentOnly2026}")
    private String jwtSecret;

    /** Access token TTL in seconds. Backwards-compatible with the old jwt.expiration property. */
    @Value("${jwt.access.expiration:${jwt.expiration:900}}")
    private long accessTokenExpirationSec;

    /** Refresh token TTL in seconds (default 7 days). */
    @Value("${jwt.refresh.expiration:604800}")
    private long refreshTokenExpirationSec;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
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
        claims.put("role", role);
        claims.put("roles", new String[]{role});
        claims.put("type", "access");
        claims.put("jti", UUID.randomUUID().toString());
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
        claims.put("role", role);
        claims.put("type", "refresh");
        claims.put("jti", UUID.randomUUID().toString());
        return createToken(claims, userId.toString(), refreshTokenExpirationSec);
    }

    /** Backwards-compatible alias used in older tests / code paths. */
    public String generateToken(Long userId, String username, String email, String role) {
        return generateAccessToken(userId, username, email, role);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationSec) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSec * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /* ────────────────────────────────────────────────────────────────────── *
     *  Claim extraction
     * ────────────────────────────────────────────────────────────────────── */

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(getAllClaimsFromToken(token).getSubject());
    }

    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token).get("username", String.class);
    }

    public String getRoleFromToken(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
    }

    /** "access" or "refresh"; null for legacy tokens that did not carry the claim. */
    public String getTypeFromToken(String token) {
        Object t = getAllClaimsFromToken(token).get("type");
        return t == null ? null : t.toString();
    }

    public String getJtiFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        String jti = claims.getId();
        if (jti == null) {
            Object o = claims.get("jti");
            jti = o == null ? null : o.toString();
        }
        return jti;
    }

    public Date getExpirationDateFromToken(String token) {
        return getAllClaimsFromToken(token).getExpiration();
    }

    /* ────────────────────────────────────────────────────────────────────── *
     *  Validation
     * ────────────────────────────────────────────────────────────────────── */

    public Boolean isTokenExpired(String token) {
        try {
            return getExpirationDateFromToken(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessTokenExpirationSec() {
        return accessTokenExpirationSec;
    }

    public long getRefreshTokenExpirationSec() {
        return refreshTokenExpirationSec;
    }
}
