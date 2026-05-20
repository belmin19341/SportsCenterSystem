package ba.nwt.apigateway.security;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Minimal PEM loader for the RSA public key the gateway uses to verify JWTs.
 *
 * <p>The gateway never sees the private key — it can only verify, not forge,
 * tokens. This is the asymmetric trust boundary required by the corrected
 * security architecture.</p>
 */
final class PemKeyLoader {

    private static final ResourceLoader RESOURCES = new DefaultResourceLoader();

    private PemKeyLoader() {}

    static RSAPublicKey loadPublic(String location) {
        Resource resource = RESOURCES.getResource(location);
        try (InputStream in = resource.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key from " + location, e);
        }
    }
}


