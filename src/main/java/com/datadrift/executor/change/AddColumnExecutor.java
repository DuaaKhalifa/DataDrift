package com.datadrift.executor.change;

import com.datadrift.model.change.AddColumnChange;
import com.datadrift.model.change.CreateTableChange.ColumnConfig;
import com.datadrift.sql.ColumnDefinitionBuilder;
import com.datadrift.sql.dialect.SqlDialect;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("addColumn")
@RequiredArgsConstructor
public class AddColumnExecutor implements ChangeExecutor<AddColumnChange> {

    private final JdbcTemplate jdbcTemplate;
    private final SqlDialect sqlDialect;

    @Override
    public void execute(AddColumnChange change) {
        List<String> statements = generateSqlStatements(change);
        String qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());

        log.info("Executing ADD COLUMN on table: {}", qualifiedTableName);
        log.debug("SQL statements: {}", statements);

        for (String sql : statements) {
            jdbcTemplate.execute(sql);
        }

        log.info("Successfully added {} column(s) to table: {}", change.getColumns().size(), qualifiedTableName);
    }

    @Override
    public String generateSql(AddColumnChange change) {
        List<String> statements = generateSqlStatements(change);
        return String.join(";" + System.lineSeparator(), statements);
    }

    private List<String> generateSqlStatements(AddColumnChange change) {
        List<String> statements = new ArrayList<>();
        ColumnDefinitionBuilder columnBuilder = new ColumnDefinitionBuilder(sqlDialect);

        String qualifiedTableName;
        if (change.getSchemaName() != null && !change.getSchemaName().isBlank()) {
            qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());
        } else {
            qualifiedTableName = SqlEscapeUtil.escapeIdentifier(change.getTableName());
        }

        // Generate separate ALTER TABLE statement for each column
        for (ColumnConfig column : change.getColumns()) {
            StringBuilder sql = new StringBuilder();
            sql.append("ALTER TABLE ");
            sql.append(qualifiedTableName);
            sql.append(" ADD COLUMN ");

            String columnDef = columnBuilder.build(column);
            sql.append(columnDef);

            statements.add(sql.toString());
        }

        return statements;
    }
}
