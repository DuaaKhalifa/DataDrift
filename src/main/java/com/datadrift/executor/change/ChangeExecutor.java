package com.datadrift.executor.change;

import com.datadrift.model.change.Change;

/**
 * Interface for executing different types of database changes.
 * Each change type (createTable, addColumn, etc.) has its own executor implementation.
 */
public interface ChangeExecutor<T extends Change> {

    /**
     * Execute a database change.
     *
     * Should:
     * 1. Generate appropriate SQL for the change
     * 2. Execute the SQL using JdbcTemplate
     * 3. Handle any database-specific syntax
     * 4. Throw exception if execution fails
     *
     * @param change The change to execute
     */
    void execute(T change);

    /**
     * Generate SQL for a change without executing it.
     *
     * Should:
     * 1. Generate the SQL statement(s) for the change
     * 2. Return as a string (for preview/logging)
     *
     * @param change The change to generate SQL for
     * @return SQL statement(s)
     */
    String generateSql(T change);
}
