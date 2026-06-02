package ba.nwt.userservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
final class PemKeyLoader {

    private static final ResourceLoader RESOURCES = new DefaultResourceLoader();

    private PemKeyLoader() {}

    static void ensureKeysExist(String privateLocation, String publicLocation) {
        if (RESOURCES.getResource(privateLocation).exists()) {
            return;
        }

        log.warn("!!! SECURITY WARNING: JWT RSA keys not found at {}. Generating new keys for development...", privateLocation);

        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();

            saveKey(privateLocation, "PRIVATE KEY", pair.getPrivate().getEncoded());
            saveKey(publicLocation, "PUBLIC KEY", pair.getPublic().getEncoded());

            log.info("Successfully generated and stored RSA key pair.");
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate RSA key pair", e);
        }
    }

    private static void saveKey(String location, String label, byte[] encoded) throws IOException {
        String pem = "-----BEGIN " + label + "-----\n" +
                Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded) +
                "\n-----END " + label + "-----\n";

        Path targetPath;
        String cleanLocation = location.replace("classpath:", "").replace("file:", "");
        
        if (location.startsWith("classpath:")) {
            Path root = Paths.get("").toAbsolutePath();
            
            if (Files.exists(root.resolve("User Service"))) {
                targetPath = root.resolve("User Service/src/main/resources").resolve(cleanLocation);
            } else if (root.toString().endsWith("User Service")) {
                targetPath = root.resolve("src/main/resources").resolve(cleanLocation);
            } else {
                targetPath = Paths.get("src/main/resources").resolve(cleanLocation);
            }
        } else {
            targetPath = Paths.get(cleanLocation);
        }

        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }
        Files.writeString(targetPath, pem);
        log.info("Saved {} to: {}", label, targetPath.toAbsolutePath());
    }

    static RSAPrivateKey loadPrivate(String location) {
        try {
            byte[] der = readPemDer(location, "PRIVATE KEY");
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load private key: " + location, e);
        }
    }

    static RSAPublicKey loadPublic(String location) {
        try {
            byte[] der = readPemDer(location, "PUBLIC KEY");
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load public key: " + location, e);
        }
    }

    private static byte[] readPemDer(String location, String label) throws IOException {
        Resource resource = RESOURCES.getResource(location);
        try (InputStream in = resource.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem
                    .replace("-----BEGIN " + label + "-----", "")
                    .replace("-----END " + label + "-----", "")
                    .replaceAll("\\s+", "");
            return Base64.getDecoder().decode(base64);
        }
    }
}