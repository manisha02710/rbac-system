package com.rbac.config;

import com.rbac.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Temporary runner that resets the demo users' passwords to known values.
 * It will run once at application startup and then can be deleted.
 */
// @Component // Temporarily disabled after password reset
public class PasswordReset implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public PasswordReset(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Define the desired password for each username
        var passwords = java.util.Map.of(
                "admin", "Admin@123",
                "faculty", "Faculty@123",
                "student1", "Student@123",
                "student2", "Student@123"
        );

        for (var entry : passwords.entrySet()) {
            String username = entry.getKey();
            String rawPass = entry.getValue();
            try {
                var user = userRepository.findByUsername(username);
                if (user != null) {
                    String newHash = encoder.encode(rawPass);
                    userRepository.updatePassword(user.getId(), newHash);
                }
            } catch (Exception e) {
                // If a user is missing we just continue; this is temporary code
                System.err.println("Password reset failed for " + username + ": " + e.getMessage());
            }
        }
        System.out.println("Password reset done");
    }
}
