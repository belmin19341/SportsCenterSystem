package ba.nwt.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Gateway-side JWT verifier (RS256).
 *
 * <p>The gateway only holds the RSA <b>public</b> key — it can verify
 * signatures but cannot mint tokens. The signing key never leaves the
 * User Service.</p>
 *
 * <p><b>Claim policy enforced on parse:</b>
 * <ul>
 *   <li>signature must match {@code jwt-public.pem}</li>
 *   <li>{@code iss} must equal {@code jwt.issuer}</li>
 *   <li>{@code aud} must equal {@code jwt.audience}</li>
 *   <li>{@code exp} not in the past (enforced by jjwt automatically)</li>
 * </ul>
 * The {@code type} (access vs refresh) and revocation (jti) checks happen
 * in {@link JwtAuthenticationFilter} because they are context-dependent
 * (some endpoints — e.g. {@code /api/auth/refresh} — must accept refresh
 * tokens).</p>
 */
@Component
public class JwtValidator {

    @Value("${jwt.public-key-path:classpath:keys/jwt-public.pem}")
    private String publicKeyPath;

    @Value("${jwt.issuer:sports-center-user-service}")
    private String issuer;

    @Value("${jwt.audience:sports-center-api}")
    private String audience;

    private RSAPublicKey publicKey;

    @PostConstruct
    void init() {
        this.publicKey = PemKeyLoader.loadPublic(publicKeyPath);
    }

    /** Extract the raw JWT from a {@code Bearer …} header. */
    public String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    /** Verify signature + iss + aud + exp. */
    public boolean validateToken(String token) {
        try {
            getAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return getAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(getAllClaims(token).getSubject());
    }

    public String getUsernameFromToken(String token) {
        return getAllClaims(token).get("username", String.class);
    }

    /** Returns the first role (compat helper for header propagation). */
    public String getRoleFromToken(String token) {
        List<String> roles = getRolesFromToken(token);
        return roles.isEmpty() ? null : roles.get(0);
    }

    /** Returns all roles from the {@code roles} claim. */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Object raw = getAllClaims(token).get("roles");
        if (raw instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    /** Returns {@code "access"} or {@code "refresh"} from the {@code type} claim. */
    public String getTypeFromToken(String token) {
        Object t = getAllClaims(token).get("type");
        return t == null ? null : t.toString();
    }

    public String getJtiFromToken(String token) {
        return getAllClaims(token).getId();
    }

    public long getExpirationEpochMs(String token) {
        return getAllClaims(token).getExpiration().getTime();
    }
}
