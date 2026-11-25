package com.rbac.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

/**
 * Small utility to verify which plain‑text password matches the BCrypt hashes
 * stored in src/main/resources/database/seed.sql.
 *
 * Run with:
 *   mvn compile
 *   java -cp "target/classes;$(mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt -Dmdep.includeScope=runtime && cat cp.txt)" com.rbac.util.PasswordVerifier
 */
public class PasswordVerifier {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final List<String> CANDIDATES = Arrays.asList(
            "admin", "admin123",
            "faculty", "faculty123",
            "student1", "student2",
            "password", "123456",
            "admin@123", "faculty@123"
    );

    public static void main(String[] args) throws Exception {
        // Load seed.sql
        String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/database/seed.sql")));
        // Extract username -> hash pairs from the INSERT statement
        Map<String, String> userHashes = new LinkedHashMap<>();
        Pattern p = Pattern.compile("\\('([^']+)'\\s*,\\s*'([^']+)'\\s*,\\s*'[^']*'\\s*,\\s*'[^']*'\\s*,\s*TRUE\\)");
        Matcher m = p.matcher(sql);
        while (m.find()) {
            String username = m.group(1);
            String hash = m.group(2);
            userHashes.put(username, hash);
        }
        System.out.println("Found " + userHashes.size() + " users in seed.sql\n");
        for (Map.Entry<String, String> entry : userHashes.entrySet()) {
            String user = entry.getKey();
            String hash = entry.getValue();
            System.out.println("User: " + user);
            boolean anyMatch = false;
            for (String candidate : CANDIDATES) {
                if (ENCODER.matches(candidate, hash)) {
                    System.out.println("  -> Password matches: " + candidate);
                    anyMatch = true;
                }
            }
            if (!anyMatch) {
                System.out.println("  -> No candidate matched (hash may correspond to a different password)");
            }
            System.out.println();
        }
    }
}
