package com.datadrift.model.change;

import lombok.Data;

@Data
public class DropForeignKeyChange implements Change {
    private String baseTableName;
    private String baseSchemaName;
    private String constraintName;
    private Boolean cascade;
    private Boolean ifExists;

    @Override
    public String getChangeType() {
        return "dropForeignKey";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (baseTableName == null || baseTableName.isBlank()) {
            throw new IllegalArgumentException("baseTableName is required for dropForeignKey");
        }
        if (constraintName == null || constraintName.isBlank()) {
            throw new IllegalArgumentException("constraintName is required for dropForeignKey");
        }
    }
}
