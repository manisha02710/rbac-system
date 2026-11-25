package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Abstraction & Inheritance
 * Abstract base class providing common user attributes and declaring abstract role behaviors.
 * OOP Concept: Encapsulation - all fields are private with accessors.
 */
public abstract class BaseUser extends BaseEntity {

    private String username;
    private String fullName;
    private String email;

    public BaseUser() {
        super();
    }

    public BaseUser(Long id, LocalDateTime createdAt, String username, String fullName, String email) {
        super(id, createdAt);
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    /**
     * OOP Concept: Abstraction & Polymorphism
     * Abstract method implemented by each subclass to return the role-specific dashboard path.
     */
    public abstract String getDashboardPath();

    /**
     * OOP Concept: Abstraction & Polymorphism
     * Abstract method implemented by each subclass to return the user-facing role title.
     */
    public abstract String getRoleLabel();
}
