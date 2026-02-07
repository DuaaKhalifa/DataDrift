package com.datadrift.exception;

/**
 * Exception thrown when a changeset execution fails.
 */
public class ChangeSetExecutionException extends RuntimeException {

    public ChangeSetExecutionException(String message) {
        super(message);
    }

    public ChangeSetExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
