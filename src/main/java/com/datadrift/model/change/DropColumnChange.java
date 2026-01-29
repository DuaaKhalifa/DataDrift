package com.datadrift.model.change;

import lombok.Data;

import java.util.List;

/**
 * Drops a column from a table.
 *
 * CASCADE will also drop all dependent objects:
 * - Indexes using the column
 * - Constraints (FK, UNIQUE, CHECK) involving the column
 * - Views referencing the column
 * - Triggers using the column
 *
 * Without CASCADE (RESTRICT), the operation fails if any dependencies exist.
 */
@Data
public class DropColumnChange implements Change {
    private String tableName;
    private String schemaName;
    private List<String> columns;
    private Boolean cascade;
    private Boolean ifExists;

    @Override
    public String getChangeType() {
        return "dropColumn";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is required for dropColumn");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("At least one column is required for dropColumn");
        }

        // Validate each column name
        for (String columnName : columns) {
            if (columnName == null || columnName.isBlank()) {
                throw new IllegalArgumentException("Column name cannot be null or blank in dropColumn");
            }
        }
    }
}
