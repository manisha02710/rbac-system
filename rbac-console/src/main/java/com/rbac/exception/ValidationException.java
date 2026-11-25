package com.rbac.exception;

/** Custom unchecked exception: ValidationException. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
