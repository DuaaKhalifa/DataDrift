package com.datadrift.sql.dialect;

import org.springframework.stereotype.Component;

/**
 * PostgreSQL-specific SQL dialect implementation.
 */
@Component
public class PostgreSqlDialect implements SqlDialect {

    @Override
    public String getName() {
        return "PostgreSQL";
    }

    @Override
    public String getAutoIncrementSyntax() {
        return "GENERATED ALWAYS AS IDENTITY";
    }

    @Override
    public String getGeneratedColumnSyntax(String expression, boolean stored) {
        // PostgreSQL requires STORED for generated columns (no VIRTUAL option)
        return "GENERATED ALWAYS AS (" + expression + ") STORED";
    }

    @Override
    public boolean supportsIfNotExists() {
        return true;
    }

    @Override
    public boolean supportsTableComments() {
        return true;
    }

    @Override
    public String getTableCommentSyntax(String qualifiedTableName, String comment) {
        return "COMMENT ON TABLE " + qualifiedTableName + " IS " + comment;
    }

    @Override
    public String getColumnCommentSyntax(String qualifiedTableName, String columnName, String comment) {
        return "COMMENT ON COLUMN " + qualifiedTableName + "." + columnName + " IS " + comment;
    }

    @Override
    public boolean requiresSeparatedComments() {
        return true;
    }
}
