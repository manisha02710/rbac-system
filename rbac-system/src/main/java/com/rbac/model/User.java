package com.rbac.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an application user.
 * // OOP: Inheritance – extends BaseEntity to reuse id and createdAt.
 */
public class User extends BaseEntity {

    // OOP: Encapsulation – private fields with getters/setters.
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private Boolean isActive;
    // For simplicity we store role ids; a real app would use a collection of Role objects.
    private List<Long> roleIds; // OOP: Polymorphism can be applied when loading roles.
    private List<Role> roles;

    public User() {
        super();
    }

    public User(Long id, LocalDateTime createdAt, String username, String passwordHash,
                String fullName, String email, Boolean isActive, List<Long> roleIds) {
        super(id, createdAt);
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.isActive = isActive;
        this.roleIds = roleIds;
    }

    // Getters & Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Legacy getter/setter used by repository
    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // Convenience getter used by templates
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    // Optional convenience setter
    public void setActive(Boolean active) {
        this.isActive = active;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
}
