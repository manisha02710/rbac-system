package com.rbac.exception;

/**
 * OOP Concept: Exception Handling
 * Custom runtime exception thrown when a requested resource (User, Role, Mark, Attendance, Student) cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " not found with identifier: " + identifier);
    }
}
