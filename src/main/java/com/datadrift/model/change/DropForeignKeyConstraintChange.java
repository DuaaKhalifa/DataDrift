package com.datadrift.model.change;

import lombok.Data;

@Data
public class DropForeignKeyConstraintChange implements Change {
    private String baseTableName;
    private String baseSchemaName;
    private String constraintName;
    private Boolean cascade;
    private Boolean ifExists;

    @Override
    public String getChangeType() {
        return "dropForeignKeyConstraint";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (baseTableName == null || baseTableName.isBlank()) {
            throw new IllegalArgumentException("baseTableName is required for dropForeignKeyConstraint");
        }
        if (constraintName == null || constraintName.isBlank()) {
            throw new IllegalArgumentException("constraintName is required for dropForeignKeyConstraint");
        }
    }
}
