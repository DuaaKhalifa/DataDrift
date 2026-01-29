package com.datadrift.executor.change;

import com.datadrift.model.change.DropColumnChange;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("dropColumn")
@RequiredArgsConstructor
public class DropColumnExecutor implements ChangeExecutor<DropColumnChange> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void execute(DropColumnChange change) {
        List<String> statements = generateSqlStatements(change);
        String qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());

        log.info("Executing DROP COLUMN on table: {}", qualifiedTableName);
        log.debug("SQL statements: {}", statements);

        for (String sql : statements) {
            jdbcTemplate.execute(sql);
        }

        log.info("Successfully dropped {} column(s) from table: {}", change.getColumns().size(), qualifiedTableName);
    }

    @Override
    public String generateSql(DropColumnChange change) {
        List<String> statements = generateSqlStatements(change);
        return String.join(";" + System.lineSeparator(), statements);
    }

    private List<String> generateSqlStatements(DropColumnChange change) {
        List<String> statements = new ArrayList<>();

        String qualifiedTableName;
        if (change.getSchemaName() != null && !change.getSchemaName().isBlank()) {
            qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());
        } else {
            qualifiedTableName = SqlEscapeUtil.escapeIdentifier(change.getTableName());
        }

        // Generate separate ALTER TABLE statement for each column
        for (String columnName : change.getColumns()) {
            StringBuilder sql = new StringBuilder();
            sql.append("ALTER TABLE ");
            sql.append(qualifiedTableName);
            sql.append(" DROP COLUMN ");

            if (Boolean.TRUE.equals(change.getIfExists())) {
                sql.append("IF EXISTS ");
            }

            sql.append(SqlEscapeUtil.escapeIdentifier(columnName));

            if (Boolean.TRUE.equals(change.getCascade())) {
                sql.append(" CASCADE");
            }

            statements.add(sql.toString());
        }

        return statements;
    }
}
