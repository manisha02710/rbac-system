package com.rbac.exception;

/** Wraps SQL errors with a friendly message. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message) { super(message); }
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
