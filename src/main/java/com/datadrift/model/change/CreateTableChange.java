package com.datadrift.model.change;

import lombok.Data;

import java.util.List;
import java.util.Objects;

/**
 * Represents a CREATE TABLE change.
 */
@Data
public class CreateTableChange implements Change {
    private String tableName;
    private String schemaName;
    private String remarks;
    private List<ColumnConfig> columns;
    private List<String> primaryKeyColumns = List.of();
    private Boolean ifNotExist;
    private List<UniqueConstraint> uniqueConstraints;
    private List<TableCheckConstraint> checkConstraints;

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
        boolean hasColumnLevelPk = columns.stream().anyMatch(col -> Objects.nonNull(col.getConstraints())
        && Boolean.TRUE.equals(col.getConstraints().isPrimaryKey()));
        boolean hasTableLevelPk = !primaryKeyColumns.isEmpty();
        if (hasColumnLevelPk && hasTableLevelPk) {
            throw new IllegalArgumentException(
                    "Cannot have both column-level and table-level primary key. Use column-level for single PK, table-level for composite PK."
            );
        }
        if (!hasColumnLevelPk && !hasTableLevelPk) {
            throw new IllegalArgumentException(
                    "A primary key is required. Define it at column-level or table-level."
            );
        }
    }

    @Data
    public static class ColumnConfig {
        private String name;
        private String type;
        private Object defaultValue;
        private Boolean autoIncrement;
        private String defaultValueComputed;
        private ConstraintsConfig constraints;
        private String remarks;
    }

    public record ConstraintsConfig(
            Boolean nullable,
            Boolean isPrimaryKey,
            Boolean isUnique,
            String foreignKeyName,
            String references,
            String checkConstraint,
            ForeignKeyAction onDelete,
            ForeignKeyAction onUpdate) {
        public Boolean isPrimaryKey(){
            return isPrimaryKey;
        }
        public Boolean hasForeignKey() {
            return references != null && !references.isBlank();
        }
    }

    public enum ForeignKeyAction {
        CASCADE,
        SET_NULL,
        RESTRICT,
        NO_ACTION,
        SET_DEFAULT;

        public String toSql(){
            return name().replace("_", " ");
        }
    }

    @Data
    public static class UniqueConstraint {
        private String constraintName;
        private List<String> columns;
    }

    @Data
    public static class TableCheckConstraint {
        private String constraintName;
        private String checkExpression;
    }
}
