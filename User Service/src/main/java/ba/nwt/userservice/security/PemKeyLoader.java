package ba.nwt.userservice.security;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Minimal PEM loader for RSA keys.
 *
 * <p>Reads PKCS#8 private keys and X.509 SubjectPublicKeyInfo public keys
 * exported by {@code openssl}. No BouncyCastle dependency required — the
 * standard JDK {@code KeyFactory} understands both encodings once the PEM
 * envelope and whitespace are stripped.</p>
 *
 * <p>Supported locations (Spring resource syntax):
 * {@code classpath:keys/jwt-private.pem}, {@code file:/etc/keys/...},
 * or a plain filesystem path.</p>
 */
final class PemKeyLoader {

    private static final ResourceLoader RESOURCES = new DefaultResourceLoader();

    private PemKeyLoader() {}

    static RSAPrivateKey loadPrivate(String location) {
        byte[] der = readPemDer(location, "PRIVATE KEY");
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key from " + location, e);
        }
    }

    static RSAPublicKey loadPublic(String location) {
        byte[] der = readPemDer(location, "PUBLIC KEY");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key from " + location, e);
        }
    }

    private static byte[] readPemDer(String location, String label) {
        Resource resource = RESOURCES.getResource(location);
        try (InputStream in = resource.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem
                    .replace("-----BEGIN " + label + "-----", "")
                    .replace("-----END " + label + "-----", "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(base64);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read key from " + location, e);
        }
    }
}

