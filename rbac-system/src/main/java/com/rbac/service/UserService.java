package com.rbac.service;

import com.rbac.exception.DuplicateResourceException;
import com.rbac.exception.InvalidCredentialsException; // Not used here but keep imports tidy
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.Role;
import com.rbac.model.User;
import com.rbac.model.FacultyProfile;
import com.rbac.repository.UserRepository;
import com.rbac.repository.RoleRepository;
import com.rbac.repository.FacultyProfileRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

/**
 * Service containing business logic for user management.
 * // OOP: Encapsulation – only services expose operations.
 * // OOP: Transaction – @Transactional ensures atomic multi‑step changes.
 */
@Service
@Validated
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // OOP: Polymorphism

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       FacultyProfileRepository facultyProfileRepository,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.auditService = auditService;
    }

    /** List all users with their assigned roles */
    public List<User> listUsers() {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            u.setRoles(roleRepository.findRolesByUserId(u.getId()));
        }
        return users;
    }

    /** List all available roles in the system */
    public List<Role> listAllRoles() {
        return roleRepository.findAll();
    }

    /** Create a new user with a BCrypt‑hashed password */
    @Transactional
    public User createUser(String username, String rawPassword, String fullName, String email, boolean isActive) {
        String hash = passwordEncoder.encode(rawPassword);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hash);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setIsActive(isActive);
        Long id = userRepository.save(user);
        user.setId(id);
        auditService.log(id, "USER_CREATE", "Created user " + username, com.rbac.model.AuditLog.Status.SUCCESS);
        return user;
    }

    /** Enable or disable a user */
    @Transactional
    public void setActive(Long userId, boolean active) {
        userRepository.setActive(userId, active);
        auditService.log(userId, "USER_DISABLE", "Set active=" + active, com.rbac.model.AuditLog.Status.SUCCESS);
    }

    /** Assign a role to a user */
    @Transactional
    public void assignRole(Long userId, Long roleId) {
        if (roleRepository.hasRole(userId, roleId)) {
            Role role = roleRepository.findById(roleId);
            String roleName = (role != null) ? role.getRoleName() : ("ID " + roleId);
            throw new IllegalArgumentException("User already has the " + roleName + " role assigned.");
        }
        roleRepository.assignRoleToUser(userId, roleId);
        auditService.log(userId, "ROLE_ASSIGN", "Assigned roleId=" + roleId, com.rbac.model.AuditLog.Status.SUCCESS);
    }

    /** Remove a role from a user */
    @Transactional
    public void removeRole(Long userId, Long roleId) {
        Role role = roleRepository.findById(roleId);
        if (role != null && "ADMIN".equalsIgnoreCase(role.getRoleName())) {
            int adminCount = roleRepository.countUsersWithRole("ADMIN");
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the ADMIN role from the last remaining system administrator.");
            }
        }
        roleRepository.removeRoleFromUser(userId, roleId);
        auditService.log(userId, "ROLE_REMOVE", "Removed roleId=" + roleId, com.rbac.model.AuditLog.Status.SUCCESS);
    }

    /**
     * Reset a user's password.
     * Validates minimum length of 6 characters, hashes with BCrypt, updates DB, and writes an audit log entry.
     */
    @Transactional
    public void resetPassword(Long targetUserId, String newPassword, Long currentUserId) {
        if (targetUserId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        User targetUser = userRepository.findById(targetUserId);
        if (targetUser == null) {
            throw new ResourceNotFoundException("User", targetUserId);
        }

        String hash = passwordEncoder.encode(newPassword.trim());
        userRepository.updatePassword(targetUserId, hash);

        auditService.log(currentUserId, "USER_PASSWORD_RESET",
                "Reset password for user " + targetUser.getUsername() + " (ID: " + targetUserId + ")",
                com.rbac.model.AuditLog.Status.SUCCESS);
    }

    /** List all faculty profiles with departments and assigned courses */
    public List<FacultyProfile> listFacultyProfiles() {
        return facultyProfileRepository.findAllFacultyWithDetails();
    }

    /** Find a faculty profile by user id */
    public FacultyProfile getFacultyProfileByUserId(Long userId) {
        return facultyProfileRepository.findByUserId(userId);
    }

    /** List all faculty users (legacy) */
    public List<User> listFacultyUsers() {
        return userRepository.findUsersByRoleName("FACULTY");
    }

    /** Create a new faculty user with BCrypt-hashed password, FACULTY role, department, and courses */
    @Transactional
    public User createFaculty(String username, String rawPassword, String fullName, String email,
                              Long departmentId, List<Long> courseIds, Long currentUserId) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (departmentId == null) {
            throw new IllegalArgumentException("Department selection is required.");
        }
        if (courseIds == null || courseIds.isEmpty()) {
            throw new IllegalArgumentException("At least one course must be assigned to the faculty member.");
        }

        username = username.trim();
        email = email.trim();

        if (userRepository.findByUsername(username) != null) {
            throw new DuplicateResourceException("User", "username", username);
        }
        if (userRepository.findByEmail(email) != null) {
            throw new DuplicateResourceException("User", "email", email);
        }

        // 1. Create User
        String hash = passwordEncoder.encode(rawPassword);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hash);
        user.setFullName(fullName.trim());
        user.setEmail(email);
        user.setIsActive(true);
        Long id = userRepository.save(user);
        user.setId(id);

        // 2. Assign FACULTY role
        com.rbac.model.Role facultyRole = roleRepository.findByName("FACULTY");
        if (facultyRole != null) {
            roleRepository.assignRoleToUser(id, facultyRole.getId());
        }

        // 3. Create Faculty Profile
        Long facultyProfileId = facultyProfileRepository.saveProfile(id, departmentId);

        // 4. Assign Courses
        for (Long courseId : courseIds) {
            if (courseId != null) {
                facultyProfileRepository.assignCourse(facultyProfileId, courseId);
            }
        }

        auditService.log(currentUserId, "USER_CREATE", "Created faculty user " + username + " with departmentId=" + departmentId, com.rbac.model.AuditLog.Status.SUCCESS);
        return user;
    }

    /** Overload for backwards compatibility */
    @Transactional
    public User createFaculty(String username, String rawPassword, String fullName, String email, Long currentUserId) {
        return createFaculty(username, rawPassword, fullName, email, null, List.of(), currentUserId);
    }

    /**
     * Update an existing faculty member's profile, department, and assigned courses in ONE transaction.
     */
    @Transactional
    public void updateFaculty(Long targetUserId, String fullName, String email,
                              Long departmentId, List<Long> courseIds, Long currentUserId) {
        User targetUser = userRepository.findById(targetUserId);
        if (targetUser == null) {
            throw new ResourceNotFoundException("Faculty user", targetUserId);
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (departmentId == null) {
            throw new IllegalArgumentException("Department selection is required.");
        }
        if (courseIds == null || courseIds.isEmpty()) {
            throw new IllegalArgumentException("At least one course must be assigned.");
        }

        fullName = fullName.trim();
        email = email.trim();

        // Check email uniqueness if changed
        User existingEmailUser = userRepository.findByEmail(email);
        if (existingEmailUser != null && !existingEmailUser.getId().equals(targetUserId)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        // 1. Update User info
        targetUser.setFullName(fullName);
        targetUser.setEmail(email);
        userRepository.update(targetUser);

        // 2. Update or create FacultyProfile
        FacultyProfile profile = facultyProfileRepository.findByUserId(targetUserId);
        Long facultyProfileId;
        if (profile != null && profile.getId() != null) {
            facultyProfileId = profile.getId();
            facultyProfileRepository.updateDepartment(facultyProfileId, departmentId);
        } else {
            facultyProfileId = facultyProfileRepository.saveProfile(targetUserId, departmentId);
        }

        // 3. Clear and re-assign courses
        facultyProfileRepository.clearCourses(facultyProfileId);
        for (Long courseId : courseIds) {
            if (courseId != null) {
                facultyProfileRepository.assignCourse(facultyProfileId, courseId);
            }
        }

        auditService.log(currentUserId, "USER_UPDATE", "Updated faculty profile for " + targetUser.getUsername(), com.rbac.model.AuditLog.Status.SUCCESS);
    }

    /**
     * Delete a faculty user.
     * Enforces that only FACULTY users can be deleted (never ADMIN or STUDENT).
     * If foreign keys block the delete, disables the account instead and returns false.
     * Returns true if deleted, false if disabled instead.
     */
    @Transactional
    public boolean deleteFaculty(Long targetUserId, Long currentUserId) {
        User targetUser = userRepository.findById(targetUserId);
        if (targetUser == null) {
            throw new ResourceNotFoundException("Faculty user", targetUserId);
        }

        List<com.rbac.model.Role> roles = roleRepository.findRolesByUserId(targetUserId);
        boolean isFaculty = roles.stream().anyMatch(r -> "FACULTY".equalsIgnoreCase(r.getRoleName()));
        boolean isAdmin = roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getRoleName()));
        boolean isStudent = roles.stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getRoleName()));

        if (!isFaculty || isAdmin || isStudent) {
            throw new IllegalArgumentException("Only faculty accounts can be deleted through this action.");
        }

        try {
            userRepository.deleteById(targetUserId);
            auditService.log(currentUserId, "USER_DISABLE", "Deleted faculty user " + targetUser.getUsername(), com.rbac.model.AuditLog.Status.SUCCESS);
            return true;
        } catch (Exception ex) {
            // Foreign keys or references block delete -> disable the account safely instead
            userRepository.setActive(targetUserId, false);
            auditService.log(currentUserId, "USER_DISABLE", "Disabled faculty user " + targetUser.getUsername() + " (deletion blocked by dependent records)", com.rbac.model.AuditLog.Status.SUCCESS);
            return false;
        }
    }

    /** Find a user by id (optional) */
    public Optional<User> findById(Long id) {
        try {
            return Optional.ofNullable(userRepository.findById(id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
