package com.datadrift.exception;

/**
 * Exception thrown when a checksum mismatch is detected.
 * Indicates a changeset was modified after it was executed.
 */
public class ChecksumMismatchException extends RuntimeException {

    public ChecksumMismatchException(String message) {
        super(message);
    }

    public ChecksumMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
