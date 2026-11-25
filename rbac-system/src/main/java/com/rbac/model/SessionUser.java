package com.rbac.model;

import java.util.Collections;
import java.util.Set;

/**
 * A lightweight object stored in HttpSession after successful login.
 * // OOP: Encapsulation – fields are private with getters.
 */
public class SessionUser {
    private final Long userId;
    private final String username;
    private final String fullName;
    private final Set<String> permissions;
    private final Set<String> roles;

    public SessionUser(Long userId, String username, String fullName, Set<String> permissions, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.permissions = permissions != null ? permissions : Collections.emptySet();
        this.roles = roles != null ? roles : Collections.emptySet();
    }

    public SessionUser(Long userId, String username, String fullName, Set<String> permissions) {
        this(userId, username, fullName, permissions, Collections.emptySet());
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String roleName) {
        return roles != null && roles.contains(roleName);
    }

    public boolean isStudent() {
        return roles != null && roles.contains("STUDENT") && !roles.contains("ADMIN") && !roles.contains("FACULTY");
    }

    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }

    public boolean isFaculty() {
        return roles != null && roles.contains("FACULTY");
    }
}
