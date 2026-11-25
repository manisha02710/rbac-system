package com.rbac.exception;

/**
 * Thrown when an uploaded file violates size, extension, or content-type rules.
 */
public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}
