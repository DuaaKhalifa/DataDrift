package com.datadrift.sql;

import com.datadrift.model.change.CreateTableChange;
import com.datadrift.model.change.CreateTableChange.ColumnConfig;
import com.datadrift.util.SqlEscapeUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for table-level constraints (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK).
 * Handles constraint generation securely with proper escaping.
 */
public class ConstraintBuilder {

    public String buildPrimaryKey(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Primary key columns cannot be empty");
        }
        String columnList = columns.stream()
                .map(SqlEscapeUtil::escapeIdentifier)
                .collect(Collectors.joining(", "));
        return "PRIMARY KEY (" + columnList + ")";
    }

    public String buildForeignKey(ColumnConfig column) {
        var constraints = column.getConstraints();

        if (constraints == null || !constraints.hasForeignKey()) {
            throw new IllegalArgumentException("Column does not have foreign key configuration");
        }

        StringBuilder sb = new StringBuilder();

        // Constraint name (optional)
        if (constraints.foreignKeyName() != null && !constraints.foreignKeyName().isBlank()) {
            sb.append("CONSTRAINT ")
                    .append(SqlEscapeUtil.escapeIdentifier(constraints.foreignKeyName()))
                    .append(" ");
        }

        // Foreign key definition
        sb.append("FOREIGN KEY (")
                .append(SqlEscapeUtil.escapeIdentifier(column.getName()))
                .append(") ");

        // References (validate it follows the pattern "table(column)" or "schema.table(column)")
        String references = constraints.references();
        validateReferencesClause(references);
        sb.append("REFERENCES ").append(references);

        // ON DELETE action
        if (constraints.onDelete() != null) {
            sb.append(" ON DELETE ").append(constraints.onDelete().toSql());
        }

        // ON UPDATE action
        if (constraints.onUpdate() != null) {
            sb.append(" ON UPDATE ").append(constraints.onUpdate().toSql());
        }

        return sb.toString();
    }

    public String buildUnique(CreateTableChange.UniqueConstraint uniqueConstraint) {
        if (uniqueConstraint.getColumns() == null || uniqueConstraint.getColumns().isEmpty()) {
            throw new IllegalArgumentException("Unique constraint must have at least one column");
        }

        StringBuilder sb = new StringBuilder();

        // Constraint name (optional)
        if (uniqueConstraint.getConstraintName() != null && !uniqueConstraint.getConstraintName().isBlank()) {
            sb.append("CONSTRAINT ")
                    .append(SqlEscapeUtil.escapeIdentifier(uniqueConstraint.getConstraintName()))
                    .append(" ");
        }

        // UNIQUE definition
        String columnList = uniqueConstraint.getColumns().stream()
                .map(SqlEscapeUtil::escapeIdentifier)
                .collect(Collectors.joining(", "));
        sb.append("UNIQUE (").append(columnList).append(")");

        return sb.toString();
    }

    public String buildCheck(CreateTableChange.TableCheckConstraint checkConstraint) {
        if (checkConstraint.getCheckExpression() == null || checkConstraint.getCheckExpression().isBlank()) {
            throw new IllegalArgumentException("Check constraint must have an expression");
        }

        StringBuilder sb = new StringBuilder();

        // Constraint name (optional)
        if (checkConstraint.getConstraintName() != null && !checkConstraint.getConstraintName().isBlank()) {
            sb.append("CONSTRAINT ")
                    .append(SqlEscapeUtil.escapeIdentifier(checkConstraint.getConstraintName()))
                    .append(" ");
        }

        // CHECK definition - validate expression for safety
        String expression = checkConstraint.getCheckExpression();
        SqlEscapeUtil.validateExpression(expression);
        sb.append("CHECK (").append(expression).append(")");

        return sb.toString();
    }

    private void validateReferencesClause(String references) {
        if (references == null || references.isBlank()) {
            throw new IllegalArgumentException("References clause cannot be empty");
        }
        // Basic validation - should contain table name and column(s) in parentheses
        if (!references.matches("^[a-zA-Z_\".]+ *\\([a-zA-Z0-9_\", ]+\\)$")) {
            throw new IllegalArgumentException(
                    "Invalid REFERENCES clause format: " + references +
                    ". Expected format: table(column) or schema.table(column)"
            );
        }
    }
}
