package com.datadrift.model.change;

import lombok.Data;

@Data
public class DropTableChange implements Change {
    private String tableName;
    private String schemaName;
    private Boolean cascade;
    private Boolean ifExists;

    @Override
    public String getChangeType() {
        return "dropTable";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is required for dropTable");
        }
    }
}
