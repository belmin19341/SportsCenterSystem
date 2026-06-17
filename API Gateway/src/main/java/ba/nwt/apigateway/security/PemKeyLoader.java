package ba.nwt.apigateway.security;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
final class PemKeyLoader {

    private PemKeyLoader() {}

    /**
     * Load RSA public key from filesystem.
     * Fails startup with clear error if file does not exist.
     */
    static RSAPublicKey loadPublic(String location) {
        try {
            log.info("Loading JWT public key from: {}", location);

            if (location != null && location.startsWith("classpath:")) {
                String resourcePath = location.substring("classpath:".length());
                try (InputStream in = PemKeyLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        String errorMsg = "FATAL: JWT public key not found on classpath: " + resourcePath;
                        log.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                    byte[] der = readPemDerFromStream(in, "PUBLIC KEY");
                    RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                            .generatePublic(new X509EncodedKeySpec(der));
                    log.info("Successfully loaded JWT public key from classpath: {}", resourcePath);
                    return publicKey;
                }
            }

            String filePath = stripScheme(location);
            log.info("Loading JWT public key from: {}", filePath);

            // Validate file exists
            if (!Files.exists(Paths.get(filePath))) {
                String errorMsg = String.format(
                    "FATAL: JWT public key not found at '%s'. Please ensure the key exists at this filesystem path.",
                    filePath
                );
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            byte[] der = readPemDer(filePath, "PUBLIC KEY");
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));

            log.info("Successfully loaded JWT public key from: {}", filePath);
            return publicKey;

        } catch (Exception e) {
            String errorMsg = "Failed to load RSA public key from " + location;
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * Strip 'file:' or 'classpath:' prefix from location path.
     */
    private static String stripScheme(String location) {
        if (location == null) {
            return "";
        }
        if (location.startsWith("file:")) {
            return location.substring(5);
        }
        // leave classpath values untouched; caller handles classpath explicitly
        return location;
    }

    private static byte[] readPemDerFromStream(InputStream in, String label) throws IOException {
        String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        String base64 = pem
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Read PEM file and extract DER-encoded key bytes.
     */
    private static byte[] readPemDer(String filePath, String label) throws IOException {
        String pem = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        String base64 = pem
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(base64);
    }
}