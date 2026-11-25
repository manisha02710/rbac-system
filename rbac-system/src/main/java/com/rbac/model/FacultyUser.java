package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance & Polymorphism
 * Subclass of {@link BaseUser} representing a Faculty Member.
 */
public class FacultyUser extends BaseUser {

    public FacultyUser() {
        super();
    }

    public FacultyUser(Long id, LocalDateTime createdAt, String username, String fullName, String email) {
        super(id, createdAt, username, fullName, email);
    }

    public FacultyUser(Long id, String username, String fullName, String email) {
        super(id, LocalDateTime.now(), username, fullName, email);
    }

    @Override
    public String getDashboardPath() {
        return "/dashboard";
    }

    @Override
    public String getRoleLabel() {
        return "Faculty Member";
    }
}
