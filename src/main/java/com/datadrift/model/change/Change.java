package com.datadrift.model.change;

/**
 * Base interface for all database changes.
 * Each change type (createTable, addColumn, etc.) implements this interface.
 */
public interface Change {
    /**
     * Returns the type of change (e.g., "createTable", "addColumn")
     */
    String getChangeType();

    /**
     * Validates that the change configuration is correct
     */
    void validate() throws IllegalArgumentException;
}
