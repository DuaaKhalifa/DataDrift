package com.datadrift.model.change;

import lombok.Data;

import java.util.List;

import static com.datadrift.model.change.CreateTableChange.ColumnConfig;

@Data
public class AddColumnChange implements Change {
    private String tableName;
    private String schemaName;
    private List<ColumnConfig> columns;

    @Override
    public String getChangeType() {
        return "addColumn";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is required for addColumn");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("At least one column is required for addColumn");
        }

        // Validate each column
        for (ColumnConfig column : columns) {
            if (column.getName() == null || column.getName().isBlank()) {
                throw new IllegalArgumentException("Column name is required in addColumn");
            }
            if (column.getType() == null || column.getType().isBlank()) {
                throw new IllegalArgumentException("Column type is required for column: " + column.getName());
            }
        }
    }
}
