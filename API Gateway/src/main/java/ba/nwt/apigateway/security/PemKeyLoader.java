package ba.nwt.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
final class PemKeyLoader {

    private static final ResourceLoader RESOURCE_LOADER =
            new DefaultResourceLoader();

    private PemKeyLoader() {
    }

    static RSAPublicKey loadPublic(String location) {

        try {

            Resource resource =
                    RESOURCE_LOADER.getResource(location);

            if (!resource.exists()) {
                throw new IllegalStateException(
                        "RSA public key not found: " + location);
            }

            try (InputStream in = resource.getInputStream()) {

                String pem = new String(
                        in.readAllBytes(),
                        StandardCharsets.UTF_8);

                String base64 = pem
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");

                byte[] der =
                        Base64.getDecoder().decode(base64);

                RSAPublicKey publicKey =
                        (RSAPublicKey) KeyFactory
                                .getInstance("RSA")
                                .generatePublic(
                                        new X509EncodedKeySpec(der));

                log.info("RSA public key loaded successfully from {}",
                        location);

                return publicKey;
            }

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to load RSA public key from " + location,
                    e);
        }
    }
}