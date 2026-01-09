package com.datadrift.model.change;

import lombok.Data;

import java.util.List;

/**
 * Represents a CREATE TABLE change.
 */
@Data
public class CreateTableChange implements Change {
    private String tableName;
    private String schemaName;
    private String remarks;
    private List<ColumnConfig> columns;

    @Override
    public String getChangeType() {
        return "createTable";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is required for createTable");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("At least one column is required for createTable");
        }
    }

    @Data
    public static class ColumnConfig {
        private String name;
        private String type;
        private Object defaultValue;
        private String defaultValueComputed;
        private ConstraintsConfig constraints;
        private String remarks;
    }

    @Data
    public static class ConstraintsConfig {
        private Boolean nullable;
        private Boolean primaryKey;
        private Boolean unique;
        private String foreignKeyName;
        private String references;
        private String checkConstraint;
    }
}
