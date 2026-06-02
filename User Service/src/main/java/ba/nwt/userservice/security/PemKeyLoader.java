package ba.nwt.userservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
final class PemKeyLoader {

    private PemKeyLoader() {}

    /**
     * Load RSA private key from filesystem.
     * Fails startup with clear error if file does not exist.
     */
    static RSAPrivateKey loadPrivate(String location) {
        try {
            String filePath = stripScheme(location);
            log.info("Loading JWT private key from: {}", filePath);
            
            // Validate file exists
            if (!Files.exists(Paths.get(filePath))) {
                String errorMsg = String.format(
                    "FATAL: JWT private key not found at '%s'. " +
                    "Please ensure the key exists at this filesystem path. " +
                    "Do not use classpath: paths. Keys must be loaded from disk.",
                    filePath
                );
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            byte[] der = readPemDer(filePath, "PRIVATE KEY");
            RSAPrivateKey key = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
            
            log.info("Successfully loaded JWT private key from: {}", filePath);
            return key;
        } catch (Exception e) {
            String errorMsg = "Cannot load JWT private key: " + location;
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * Load RSA public key from filesystem.
     * Fails startup with clear error if file does not exist.
     */
    static RSAPublicKey loadPublic(String location) {
        try {
            String filePath = stripScheme(location);
            log.info("Loading JWT public key from: {}", filePath);
            
            // Validate file exists
            if (!Files.exists(Paths.get(filePath))) {
                String errorMsg = String.format(
                    "FATAL: JWT public key not found at '%s'. " +
                    "Please ensure the key exists at this filesystem path. " +
                    "Do not use classpath: paths. Keys must be loaded from disk.",
                    filePath
                );
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            byte[] der = readPemDer(filePath, "PUBLIC KEY");
            RSAPublicKey key = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
            
            log.info("Successfully loaded JWT public key from: {}", filePath);
            return key;
        } catch (Exception e) {
            String errorMsg = "Cannot load JWT public key: " + location;
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * Strip 'file:' or 'classpath:' prefix from location path.
     */
    private static String stripScheme(String location) {
        if (location.startsWith("file:")) {
            return location.substring(5);
        }
        if (location.startsWith("classpath:")) {
            throw new IllegalStateException(
                "FATAL: Classpath resource paths are not supported for JWT keys. " +
                "Use 'file:' prefix or relative paths only. Got: " + location
            );
        }
        return location;
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