package com.rbac.exception;

/** Custom unchecked exception: AccessDeniedException. */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) { super(message); }
}
