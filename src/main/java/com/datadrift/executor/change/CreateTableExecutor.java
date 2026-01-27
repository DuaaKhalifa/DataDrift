package com.datadrift.executor.change;

import com.datadrift.model.change.CreateTableChange;
import com.datadrift.model.change.CreateTableChange.ColumnConfig;
import com.datadrift.sql.ColumnDefinitionBuilder;
import com.datadrift.sql.ConstraintBuilder;
import com.datadrift.sql.CreateTableSqlBuilder;
import com.datadrift.sql.dialect.SqlDialect;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("createTable")
@RequiredArgsConstructor
public class CreateTableExecutor implements ChangeExecutor<CreateTableChange> {

    private final JdbcTemplate jdbcTemplate;
    private final SqlDialect sqlDialect;

    /**
     * Execute a CREATE TABLE change.
     * Generates SQL using the builder pattern and executes all statements.
     */
    @Override
    public void execute(CreateTableChange change) {
        List<String> statements = generateSqlStatements(change);

        String qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());
        log.info("Executing CREATE TABLE: {}", qualifiedTableName);
        log.debug("SQL statements: {}", statements);

        // Execute all statements (CREATE TABLE + comments)
        for (String statement : statements) {
            if (!statement.isBlank()) {
                jdbcTemplate.execute(statement);
            }
        }

        log.info("Successfully created table: {}", qualifiedTableName);
    }

    @Override
    public String generateSql(CreateTableChange change) {
        List<String> statements = generateSqlStatements(change);
        return String.join(";" + System.lineSeparator(), statements);
    }

    private List<String> generateSqlStatements(CreateTableChange change) {
        CreateTableSqlBuilder builder = buildCreateTableSql(change);
        return builder.buildStatements();
    }

    private CreateTableSqlBuilder buildCreateTableSql(CreateTableChange change) {
        CreateTableSqlBuilder sqlBuilder = new CreateTableSqlBuilder(sqlDialect);
        ColumnDefinitionBuilder columnBuilder = new ColumnDefinitionBuilder(sqlDialect);
        ConstraintBuilder constraintBuilder = new ConstraintBuilder();

        // Set table info
        sqlBuilder
                .table(change.getTableName())
                .schema(change.getSchemaName())
                .ifNotExists(Boolean.TRUE.equals(change.getIfNotExist()));

        // Add column definitions
        for (ColumnConfig column : change.getColumns()) {
            String columnDef = columnBuilder.build(column);
            sqlBuilder.addDefinition(columnDef);

            // Add column comment if present
            if (column.getRemarks() != null && !column.getRemarks().isBlank()) {
                sqlBuilder.columnComment(column.getName(), column.getRemarks());
            }
        }

        // Add table-level primary key constraint (if composite)
        if (change.getPrimaryKeyColumns() != null && !change.getPrimaryKeyColumns().isEmpty()) {
            String pkConstraint = constraintBuilder.buildPrimaryKey(change.getPrimaryKeyColumns());
            sqlBuilder.addDefinition(pkConstraint);
        }

        // Add foreign key constraints
        for (ColumnConfig column : change.getColumns()) {
            if (column.getConstraints() != null && column.getConstraints().hasForeignKey()) {
                String fkConstraint = constraintBuilder.buildForeignKey(column);
                sqlBuilder.addDefinition(fkConstraint);
            }
        }

        // Add table-level unique constraints
        if (change.getUniqueConstraints() != null) {
            for (var uniqueConstraint : change.getUniqueConstraints()) {
                String uniqueDef = constraintBuilder.buildUnique(uniqueConstraint);
                sqlBuilder.addDefinition(uniqueDef);
            }
        }

        // Add table-level check constraints
        if (change.getCheckConstraints() != null) {
            for (var checkConstraint : change.getCheckConstraints()) {
                String checkDef = constraintBuilder.buildCheck(checkConstraint);
                sqlBuilder.addDefinition(checkDef);
            }
        }

        // Add table comment if present
        if (change.getRemarks() != null && !change.getRemarks().isBlank()) {
            sqlBuilder.tableComment(change.getRemarks());
        }

        return sqlBuilder;
    }
}
