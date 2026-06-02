package ba.nwt.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ROLES = "roles";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    @Value("${jwt.kid}")
    private String keyId;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpirationSec;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpirationSec;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @PostConstruct
    public void init() {
        log.info("Initializing JWT Provider. Checking for RSA keys...");
        
        // Osiguraj da ključevi postoje prije učitavanja
        PemKeyLoader.ensureKeysExist(privateKeyPath, publicKeyPath);

        this.privateKey = PemKeyLoader.loadPrivate(privateKeyPath);
        this.publicKey  = PemKeyLoader.loadPublic(publicKeyPath);

        log.info("RSA JWT keys loaded successfully. kid={}", keyId);
    }

    public String generateAccessToken(
            Long userId,
            String username,
            String email,
            String role) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("username", username);
        claims.put("email", email);
        claims.put(CLAIM_ROLES, List.of(role));
        claims.put(CLAIM_TYPE, TYPE_ACCESS);

        return createToken(
                claims,
                userId.toString(),
                accessTokenExpirationSec);
    }

    public String generateRefreshToken(
            Long userId,
            String username,
            String role) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("username", username);
        claims.put(CLAIM_ROLES, List.of(role));
        claims.put(CLAIM_TYPE, TYPE_REFRESH);

        return createToken(
                claims,
                userId.toString(),
                refreshTokenExpirationSec);
    }

    public String generateToken(
            Long userId,
            String username,
            String email,
            String role) {

        return generateAccessToken(
                userId,
                username,
                email,
                role);
    }

    private String createToken(
            Map<String, Object> claims,
            String subject,
            long expirationSec) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSec * 1000);

        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setHeaderParam("typ", "JWT")
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(
                getAllClaimsFromToken(token).getSubject());
    }

    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token)
                .get("username", String.class);
    }

    public String getRoleFromToken(String token) {

        Object roles =
                getAllClaimsFromToken(token)
                        .get(CLAIM_ROLES);

        if (roles instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }

        return null;
    }

    public String getTypeFromToken(String token) {

        Object type =
                getAllClaimsFromToken(token)
                        .get(CLAIM_TYPE);

        return type == null
                ? null
                : type.toString();
    }

    public String getJtiFromToken(String token) {
        return getAllClaimsFromToken(token).getId();
    }

    public Date getExpirationDateFromToken(String token) {
        return getAllClaimsFromToken(token).getExpiration();
    }

    public Boolean isTokenExpired(String token) {

        try {
            return getExpirationDateFromToken(token)
                    .before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean validateToken(String token) {

        try {
            getAllClaimsFromToken(token);
            return true;
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