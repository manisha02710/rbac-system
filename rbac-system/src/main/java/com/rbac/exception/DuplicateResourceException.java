package com.rbac.exception;

/**
 * OOP Concept: Exception Handling
 * Custom runtime exception thrown when creating or updating a resource violates unique constraints
 * (e.g. duplicate username, email, or roll number).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(resourceName + " with " + field + " '" + value + "' already exists.");
    }
}
