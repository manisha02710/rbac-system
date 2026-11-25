package com.rbac.exception;

/** Custom unchecked exception: ResourceNotFoundException. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
