package com.datadrift.exception;

/**
 * Exception thrown when migration lock cannot be acquired.
 * Indicates another process is currently running migrations.
 */
public class MigrationLockException extends RuntimeException {

    public MigrationLockException(String message) {
        super(message);
    }

    public MigrationLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
