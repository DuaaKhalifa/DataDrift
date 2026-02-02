package com.datadrift.model.change;

import lombok.Data;

@Data
public class DeleteChange implements Change {
    private String tableName;
    private String schemaName;
    private String where;

    @Override
    public String getChangeType() {
        return "delete";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is required for delete");
        }
        if (where == null || where.isBlank()) {
            throw new IllegalArgumentException("WHERE clause is required for delete (to prevent accidental full table deletes)");
        }
    }
}
