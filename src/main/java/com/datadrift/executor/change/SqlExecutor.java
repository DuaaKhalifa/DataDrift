package com.datadrift.executor.change;

import com.datadrift.model.change.SqlChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Executor for custom SQL changes.
 */
@Slf4j
@Component("sql")
@RequiredArgsConstructor
public class SqlExecutor implements ChangeExecutor<SqlChange> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void execute(SqlChange change) {
        String sql = generateSql(change);

        log.info("Executing custom SQL");

        if (change.isSplitStatements()) {
            // Split and execute multiple statements
            List<String> statements = splitSqlStatements(sql, change.getEndDelimiter());

            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    jdbcTemplate.execute(trimmed);
                }
            }
        } else {
            // Execute as single statement
            jdbcTemplate.execute(sql);
        }

        log.info("Successfully executed custom SQL");
    }

    @Override
    public String generateSql(SqlChange change) {
        return change.getSql();
    }

    private List<String> splitSqlStatements(String sql, String delimiter) {
        List<String> statements = new ArrayList<>();
        String actualDelimiter = delimiter != null ? delimiter : ";";

        String[] parts = sql.split(java.util.regex.Pattern.quote(actualDelimiter));

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }

        return statements;
    }
}
