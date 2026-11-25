package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance & Polymorphism
 * Subclass of {@link BaseUser} representing an Administrator.
 */
public class AdminUser extends BaseUser {

    public AdminUser() {
        super();
    }

    public AdminUser(Long id, LocalDateTime createdAt, String username, String fullName, String email) {
        super(id, createdAt, username, fullName, email);
    }

    public AdminUser(Long id, String username, String fullName, String email) {
        super(id, LocalDateTime.now(), username, fullName, email);
    }

    @Override
    public String getDashboardPath() {
        return "/dashboard";
    }

    @Override
    public String getRoleLabel() {
        return "Administrator";
    }
}
