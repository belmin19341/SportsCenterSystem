package ba.nwt.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Startup validation bean that ensures JWT keys exist on the filesystem
 * before the application starts.
 * 
 * Fails fast with a clear error message if keys are missing.
 */
@Component
@Slf4j
public class JwtKeyValidation implements ApplicationRunner {

    @Value("${jwt.private-key-path:classpath:keys/jwt-private.pem}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path:classpath:keys/jwt-public.pem}")
    private String publicKeyPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=".repeat(80));
        log.info("STARTUP VALIDATION: Checking JWT key files...");
        log.info("=".repeat(80));

        validateKeyExists(privateKeyPath, "PRIVATE");
        validateKeyExists(publicKeyPath, "PUBLIC");

        log.info("=".repeat(80));
        log.info("✓ JWT keys validation PASSED. All required key files exist.");
        log.info("=".repeat(80));
    }

    private void validateKeyExists(String keyPath, String keyType) {
        if (keyPath != null && keyPath.startsWith("classpath:")) {
            validateClasspathKey(keyPath, keyType);
            return;
        }

        String filePath = stripScheme(keyPath);
        boolean exists;
        try {
            exists = Files.exists(Paths.get(filePath));
        } catch (Exception e) {
            log.error("FATAL: JWT {} key path is invalid: '{}' — {}", keyType, filePath, e.getMessage());
            throw new IllegalStateException("JWT " + keyType + " key path is invalid: " + filePath, e);
        }

        if (!exists) {
            String errorMsg = String.format(
                "\n" +
                "╔════════════════════════════════════════════════════════════════╗\n" +
                "║  FATAL: JWT %s KEY NOT FOUND                                 ║\n" +
                "╠════════════════════════════════════════════════════════════════╣\n" +
                "║  Expected location: %s\n" +
                "║                                                                ║\n" +
                "║  Action Required:                                              ║\n" +
                "║  1. Generate RSA keys using: scripts/gen-jwt-keys.sh          ║\n" +
                "║  2. Copy keys to: ./keys/                                     ║\n" +
                "║  3. Restart the application                                   ║\n" +
                "╚════════════════════════════════════════════════════════════════╝",
                keyType, filePath
            );

            log.error(errorMsg);
            throw new IllegalStateException("JWT " + keyType + " key not found: " + filePath);
        }

        log.info("✓ JWT {} key found at: {}", keyType, filePath);
    }

    private void validateClasspathKey(String keyPath, String keyType) {
        String resourcePath = keyPath.substring("classpath:".length());
        try (var in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                log.error("FATAL: JWT {} key not found on classpath: {}", keyType, resourcePath);
                throw new IllegalStateException("JWT " + keyType + " key not found on classpath: " + resourcePath);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("FATAL: Could not read JWT {} key from classpath: {}", keyType, resourcePath, e);
            throw new IllegalStateException("JWT " + keyType + " key unreadable on classpath: " + resourcePath, e);
        }
        log.info("✓ JWT {} key found on classpath: {}", keyType, resourcePath);
    }

    private static String stripScheme(String location) {
        if (location == null) return "";
        if (location.startsWith("file:")) {
            return location.substring(5);
        }
        return location;
    }
}
