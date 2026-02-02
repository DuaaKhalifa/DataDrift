package com.datadrift.model.change;

import lombok.Data;

import java.util.List;

@Data
public class InsertChange implements Change {
    private String tableName;
    private String schemaName;
    private List<ColumnValue> columns;

    @Override
    public String getChangeType() {
        return "insert";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is required for insert");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("At least one column is required for insert");
        }

        // Validate each column
        for (ColumnValue column : columns) {
            if (column.getName() == null || column.getName().isBlank()) {
                throw new IllegalArgumentException("Column name cannot be null or blank in insert");
            }
            if (column.getValueType() == null || column.getValueType().isBlank()) {
                throw new IllegalArgumentException("Column valueType is required for column: " + column.getName());
            }
            // Validate value type
            if (!isValidValueType(column.getValueType())) {
                throw new IllegalArgumentException("Invalid valueType for column " + column.getName() + ": " + column.getValueType());
            }
            // NULL type should have null value
            if ("NULL".equalsIgnoreCase(column.getValueType()) && column.getValue() != null) {
                throw new IllegalArgumentException("Column " + column.getName() + " has valueType NULL but value is not null");
            }
        }
    }

    private boolean isValidValueType(String valueType) {
        return valueType != null && (
                valueType.equalsIgnoreCase("STRING") ||
                valueType.equalsIgnoreCase("NUMERIC") ||
                valueType.equalsIgnoreCase("BOOLEAN") ||
                valueType.equalsIgnoreCase("NULL") ||
                valueType.equalsIgnoreCase("TIMESTAMP") ||
                valueType.equalsIgnoreCase("DATE")
        );
    }

    @Data
    public static class ColumnValue {
        private String name;
        private String value;
        private String valueType;  // STRING, NUMERIC, BOOLEAN, NULL, TIMESTAMP, DATE
    }
}
