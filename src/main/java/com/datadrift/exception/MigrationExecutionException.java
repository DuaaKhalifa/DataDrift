package com.datadrift.exception;

/**
 * Exception thrown when migration execution fails.
 */
public class MigrationExecutionException extends RuntimeException {

    public MigrationExecutionException(String message) {
        super(message);
    }

    public MigrationExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
