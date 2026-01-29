package com.datadrift.model.change;

import lombok.Data;

import java.util.List;

@Data
public class CreateIndexChange implements Change {
    private String indexName;
    private String tableName;
    private String schemaName;
    private Boolean unique;
    private List<String> columns;

    @Override
    public String getChangeType() {
        return "createIndex";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is required for createIndex");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("At least one column is required for createIndex");
        }
        // Validate that column names are not blank
        for (String column : columns) {
            if (column == null || column.isBlank()) {
                throw new IllegalArgumentException("Column name cannot be null or blank in createIndex");
            }
        }
    }
}
