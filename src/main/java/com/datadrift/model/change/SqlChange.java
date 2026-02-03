package com.datadrift.model.change;

import lombok.Data;

/**
 * Executes raw SQL statements.
 *
 * This change type provides maximum flexibility for complex operations that
 * cannot be expressed using structured changes:
 * - Database functions and stored procedures
 * - Triggers
 * - Views and materialized views
 * - Complex queries with CTEs, window functions, etc.
 * - Database-specific features
 *
 * WARNING: Raw SQL bypasses normal safety checks. Use with caution.
 * Ensure SQL is properly tested and does not contain SQL injection vulnerabilities.
 */
@Data
public class SqlChange implements Change {
    private String sql;
    private String dbms;
    private boolean stripComments;
    private boolean splitStatements;
    private String endDelimiter;

    @Override
    public String getChangeType() {
        return "sql";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql is required for sql change");
        }
    }
}
