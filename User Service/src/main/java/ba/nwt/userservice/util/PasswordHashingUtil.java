package ba.nwt.userservice.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Password Hashing Utility for Development
 * Use this to generate password hashes for testing
 * 
 * Run: java -cp "target/classes:..." ba.nwt.userservice.util.PasswordHashingUtil <password>
 * Or use in your IDE to execute and copy the hash
 */
public class PasswordHashingUtil {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java PasswordHashingUtil <password>");
            System.out.println("\nExample: java PasswordHashingUtil password123");
            System.out.println("\nCommon test passwords:");
            printHashesForCommonPasswords();
        } else {
            String password = args[0];
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
            String hash = encoder.encode(password);
            System.out.println("Original: " + password);
            System.out.println("Hash: " + hash);
        }
    }

    private static void printHashesForCommonPasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String[] testPasswords = {
                "password123",
                "admin123",
                "owner123",
                "user123",
                "test123"
        };

        System.out.println("\nSQL INSERT statements for testing:");
        for (String password : testPasswords) {
            String hash = encoder.encode(password);
            System.out.println("-- Password: " + password);
            System.out.println("-- Hash: " + hash);
        }
    }
}
