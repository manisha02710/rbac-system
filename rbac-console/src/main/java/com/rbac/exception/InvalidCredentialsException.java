package com.rbac.exception;

/** Custom unchecked exception: InvalidCredentialsException. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) { super(message); }
}
