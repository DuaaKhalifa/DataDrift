package com.datadrift.sql.dialect;

/**
 * Interface for database-specific SQL dialect handling.
 * Different databases have different syntax for certain operations.
 */
public interface SqlDialect {

    /**
     * Get the name of this dialect.
     */
    String getName();

    /**
     * Generate auto-increment/identity column syntax.
     *
     * @return SQL fragment for auto-increment
     */
    String getAutoIncrementSyntax();

    /**
     * Generate computed/generated column syntax.
     *
     * @param expression The computed expression
     * @param stored Whether the column is STORED or VIRTUAL
     * @return SQL fragment for generated column
     */
    String getGeneratedColumnSyntax(String expression, boolean stored);

    /**
     * Check if this dialect supports IF NOT EXISTS clause.
     *
     * @return true if supported
     */
    boolean supportsIfNotExists();

    /**
     * Get the syntax for IF NOT EXISTS.
     *
     * @return SQL fragment for IF NOT EXISTS
     */
    default String getIfNotExistsSyntax() {
        return supportsIfNotExists() ? "IF NOT EXISTS " : "";
    }

    /**
     * Check if this dialect supports table comments.
     *
     * @return true if supported
     */
    boolean supportsTableComments();

    /**
     * Get the syntax for adding a table comment.
     *
     * @param qualifiedTableName The fully qualified table name (already escaped)
     * @param comment The comment (already escaped)
     * @return SQL statement for adding table comment
     */
    String getTableCommentSyntax(String qualifiedTableName, String comment);

    /**
     * Get the syntax for adding a column comment.
     *
     * @param qualifiedTableName The fully qualified table name (already escaped)
     * @param columnName The column name (already escaped)
     * @param comment The comment (already escaped)
     * @return SQL statement for adding column comment
     */
    String getColumnCommentSyntax(String qualifiedTableName, String columnName, String comment);

    /**
     * Check if this dialect requires separated comment statements.
     *
     * @return true if comments must be in separate statements after CREATE TABLE
     */
    boolean requiresSeparatedComments();
}
