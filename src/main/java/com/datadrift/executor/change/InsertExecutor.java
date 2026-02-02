package com.datadrift.executor.change;

import com.datadrift.model.change.InsertChange;
import com.datadrift.model.change.InsertChange.ColumnValue;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component("insert")
@RequiredArgsConstructor
public class InsertExecutor implements ChangeExecutor<InsertChange> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void execute(InsertChange change) {
        String sql = generateSql(change);
        String qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());

        log.info("Executing INSERT into table: {}", qualifiedTableName);
        log.debug("SQL statement: {}", sql);

        jdbcTemplate.execute(sql);

        log.info("Successfully inserted 1 row into table: {}", qualifiedTableName);
    }

    @Override
    public String generateSql(InsertChange change) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");

        // Table name
        String qualifiedTableName;
        if (change.getSchemaName() != null && !change.getSchemaName().isBlank()) {
            qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());
        } else {
            qualifiedTableName = SqlEscapeUtil.escapeIdentifier(change.getTableName());
        }
        sql.append(qualifiedTableName);

        // Column names
        sql.append(" (");
        String columnNames = change.getColumns().stream()
                .map(col -> SqlEscapeUtil.escapeIdentifier(col.getName()))
                .collect(Collectors.joining(", "));
        sql.append(columnNames);
        sql.append(")");

        // Values
        sql.append(" VALUES (");
        String values = change.getColumns().stream()
                .map(this::formatValue)
                .collect(Collectors.joining(", "));
        sql.append(values);
        sql.append(")");

        return sql.toString();
    }

    private String formatValue(ColumnValue column) {
        String valueType = column.getValueType().toUpperCase();

        switch (valueType) {
            case "NULL":
                return "NULL";

            case "NUMERIC":
                // Validate it's a valid number
                if (column.getValue() == null || column.getValue().isBlank()) {
                    throw new IllegalArgumentException("NUMERIC value cannot be null or blank for column: " + column.getName());
                }
                try {
                    // Try to parse as double to validate
                    Double.parseDouble(column.getValue());
                    return column.getValue();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid NUMERIC value for column " + column.getName() + ": " + column.getValue());
                }

            case "BOOLEAN":
                if (column.getValue() == null || column.getValue().isBlank()) {
                    throw new IllegalArgumentException("BOOLEAN value cannot be null or blank for column: " + column.getName());
                }
                String boolValue = column.getValue().trim().toLowerCase();
                if ("true".equals(boolValue) || "false".equals(boolValue) || "1".equals(boolValue) || "0".equals(boolValue)) {
                    // Convert to true/false for PostgreSQL
                    return ("true".equals(boolValue) || "1".equals(boolValue)) ? "true" : "false";
                } else {
                    throw new IllegalArgumentException("Invalid BOOLEAN value for column " + column.getName() + ": " + column.getValue());
                }

            case "STRING":
            case "TIMESTAMP":
            case "DATE":
                // All these types need to be escaped as string literals
                if (column.getValue() == null) {
                    return "NULL";
                }
                return SqlEscapeUtil.escapeStringLiteral(column.getValue());

            default:
                throw new IllegalArgumentException("Unsupported valueType: " + valueType);
        }
    }
}
