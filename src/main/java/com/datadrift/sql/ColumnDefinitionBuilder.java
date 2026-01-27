package com.datadrift.sql;

import com.datadrift.model.change.CreateTableChange.ColumnConfig;
import com.datadrift.sql.dialect.SqlDialect;
import com.datadrift.util.SqlEscapeUtil;

import java.util.ArrayList;
import java.util.List;

public class ColumnDefinitionBuilder {

    private final SqlDialect dialect;

    public ColumnDefinitionBuilder(SqlDialect dialect) {
        this.dialect = dialect;
    }

    public String build(ColumnConfig column) {
        if (column.getName() == null || column.getName().isBlank()) {
            throw new IllegalArgumentException("Column name is required");
        }
        if (column.getType() == null || column.getType().isBlank()) {
            throw new IllegalArgumentException("Column type is required for column: " + column.getName());
        }

        List<String> parts = new ArrayList<>();

        // Column name and type
        parts.add(SqlEscapeUtil.escapeIdentifier(column.getName()) + " " + column.getType());

        // Generated/computed column (takes precedence over default)
        if (column.getDefaultValueComputed() != null && !column.getDefaultValueComputed().isBlank()) {
            SqlEscapeUtil.validateExpression(column.getDefaultValueComputed());
            parts.add(dialect.getGeneratedColumnSyntax(column.getDefaultValueComputed(), true));
        }
        // Default value
        else if (column.getDefaultValue() != null) {
            parts.add("DEFAULT " + formatDefaultValue(column.getDefaultValue()));
        }

        // Auto increment
        if (Boolean.TRUE.equals(column.getAutoIncrement())) {
            parts.add(dialect.getAutoIncrementSyntax());
        }

        // Add constraints if present
        if (column.getConstraints() != null) {
            var constraints = column.getConstraints();

            // Nullable constraint
            if (constraints.nullable() != null) {
                parts.add(constraints.nullable() ? "NULL" : "NOT NULL");
            }

            // Primary key
            if (Boolean.TRUE.equals(constraints.isPrimaryKey())) {
                parts.add("PRIMARY KEY");
            }

            // Unique
            if (Boolean.TRUE.equals(constraints.isUnique())) {
                parts.add("UNIQUE");
            }

            // Check constraint
            if (constraints.checkConstraint() != null && !constraints.checkConstraint().isBlank()) {
                SqlEscapeUtil.validateExpression(constraints.checkConstraint());
                parts.add("CHECK (" + constraints.checkConstraint() + ")");
            }
        }

        return String.join(" ", parts);
    }

    private String formatDefaultValue(Object defaultValue) {
        if (defaultValue instanceof Number || defaultValue instanceof Boolean) {
            return defaultValue.toString();
        } else if (defaultValue instanceof String) {
            String value = (String) defaultValue;
            // If it looks like a SQL function or keyword, don't quote it
            if (value.matches("(?i)^(CURRENT_TIMESTAMP|CURRENT_DATE|CURRENT_TIME|NULL|TRUE|FALSE)$")) {
                return value;
            }
            // Otherwise, escape it as a string literal
            return SqlEscapeUtil.escapeStringLiteral(value);
        }
        return defaultValue.toString();
    }
}
