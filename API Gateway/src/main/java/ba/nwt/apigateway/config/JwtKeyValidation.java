package ba.nwt.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Startup validation bean that ensures JWT public key exists on the filesystem
 * before the application starts.
 * 
 * Fails fast with a clear error message if the key is missing.
 */
@Component
@Slf4j
public class JwtKeyValidation implements ApplicationRunner {

    @Value("${jwt.public-key-path:classpath:keys/jwt-public.pem}")
    private String publicKeyPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=".repeat(80));
        log.info("STARTUP VALIDATION: Checking JWT public key file...");
        log.info("=".repeat(80));

        validateKeyExists(publicKeyPath);

        log.info("=".repeat(80));
        log.info("✓ JWT public key validation PASSED. Key file exists.");
        log.info("=".repeat(80));
    }

    private void validateKeyExists(String keyPath) {
        if (keyPath != null && keyPath.startsWith("classpath:")) {
            validateClasspathKey(keyPath);
            return;
        }

        String filePath = stripScheme(keyPath);
        boolean exists;
        try {
            exists = Files.exists(Paths.get(filePath));
        } catch (Exception e) {
            log.error("FATAL: JWT public key path is invalid: '{}' — {}", filePath, e.getMessage());
            throw new IllegalStateException("JWT public key path is invalid: " + filePath, e);
        }

        if (!exists) {
            String errorMsg = String.format(
                "\n" +
                "╔════════════════════════════════════════════════════════════════╗\n" +
                "║  FATAL: JWT PUBLIC KEY NOT FOUND                              ║\n" +
                "╠════════════════════════════════════════════════════════════════╣\n" +
                "║  Expected location: %s\n" +
                "║                                                                ║\n" +
                "║  Action Required:                                              ║\n" +
                "║  1. Copy the public key from User Service                      ║\n" +
                "║  2. Place it at: ./keys/jwt-public.pem                        ║\n" +
                "║  3. Restart the application                                   ║\n" +
                "║                                                                ║\n" +
                "║  Note:                                                         ║\n" +
                "║  - The gateway only needs the PUBLIC key                      ║\n" +
                "║  - The User Service holds the private key                     ║\n" +
                "╚════════════════════════════════════════════════════════════════╝",
                filePath
            );

            log.error(errorMsg);
            throw new IllegalStateException("JWT public key not found: " + filePath);
        }

        log.info("✓ JWT public key found at: {}", filePath);
    }

    private void validateClasspathKey(String keyPath) {
        String resourcePath = keyPath.substring("classpath:".length());
        try (var in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                log.error("FATAL: JWT public key not found on classpath: {}", resourcePath);
                throw new IllegalStateException("JWT public key not found on classpath: " + resourcePath);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("FATAL: Could not read JWT public key from classpath: {}", resourcePath, e);
            throw new IllegalStateException("JWT public key unreadable on classpath: " + resourcePath, e);
        }
        log.info("✓ JWT public key found on classpath: {}", resourcePath);
    }

    private static String stripScheme(String location) {
        if (location == null) return "";
        if (location.startsWith("file:")) {
            return location.substring(5);
        }
        return location;
    }
}
