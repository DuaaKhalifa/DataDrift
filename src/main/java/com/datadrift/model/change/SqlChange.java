package com.datadrift.model.change;

import lombok.Data;

/**
 * Represents a custom SQL change.
 * Allows execution of arbitrary SQL statements.
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
