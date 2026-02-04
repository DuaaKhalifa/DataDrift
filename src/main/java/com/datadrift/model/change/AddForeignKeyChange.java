package com.datadrift.model.change;

import lombok.Data;

import java.util.List;

@Data
public class AddForeignKeyChange implements Change {
    private String baseTableName;
    private String baseSchemaName;
    private List<String> baseColumnNames;

    private String referencedTableName;
    private String referencedSchemaName;
    private List<String> referencedColumnNames;

    private String constraintName;
    private String onUpdate;
    private String onDelete;
    private Boolean deferrable;
    private Boolean initiallyDeferred;

    @Override
    public String getChangeType() {
        return "addForeignKey";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (baseTableName == null || baseTableName.isBlank()) {
            throw new IllegalArgumentException("baseTableName is required for addForeignKey");
        }
        if (baseColumnNames == null || baseColumnNames.isEmpty()) {
            throw new IllegalArgumentException("At least one base column is required for addForeignKey");
        }
        if (referencedTableName == null || referencedTableName.isBlank()) {
            throw new IllegalArgumentException("referencedTableName is required for addForeignKey");
        }
        if (referencedColumnNames == null || referencedColumnNames.isEmpty()) {
            throw new IllegalArgumentException("At least one referenced column is required for addForeignKey");
        }
        if (baseColumnNames.size() != referencedColumnNames.size()) {
            throw new IllegalArgumentException("Number of base columns must match number of referenced columns");
        }

        // Validate column names
        for (String columnName : baseColumnNames) {
            if (columnName == null || columnName.isBlank()) {
                throw new IllegalArgumentException("Base column name cannot be null or blank");
            }
        }
        for (String columnName : referencedColumnNames) {
            if (columnName == null || columnName.isBlank()) {
                throw new IllegalArgumentException("Referenced column name cannot be null or blank");
            }
        }

        // Validate foreign key actions if provided
        if (onUpdate != null && !isValidForeignKeyAction(onUpdate)) {
            throw new IllegalArgumentException("Invalid onUpdate action: " + onUpdate);
        }
        if (onDelete != null && !isValidForeignKeyAction(onDelete)) {
            throw new IllegalArgumentException("Invalid onDelete action: " + onDelete);
        }
    }

    private boolean isValidForeignKeyAction(String action) {
        return action != null && (
                action.equalsIgnoreCase("CASCADE") ||
                action.equalsIgnoreCase("RESTRICT") ||
                action.equalsIgnoreCase("SET NULL") ||
                action.equalsIgnoreCase("SET DEFAULT") ||
                action.equalsIgnoreCase("NO ACTION")
        );
    }
}
