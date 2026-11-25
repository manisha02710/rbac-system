package com.rbac.exception;

/** Custom unchecked exception: DuplicateResourceException. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}
