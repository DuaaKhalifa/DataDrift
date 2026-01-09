package com.datadrift.executor.change;

import com.datadrift.model.change.Change;
import com.datadrift.model.change.SqlChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Executor for custom SQL changes.
 */
@Slf4j
@Component("sql")
@RequiredArgsConstructor
public class SqlExecutor implements ChangeExecutor {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Execute a custom SQL change.
     *
     * Should:
     * 1. Cast Change to SqlChange
     * 2. Get the SQL string from the change
     * 3. If splitStatements=true, split by endDelimiter (default ';')
     * 4. Execute each SQL statement using jdbcTemplate
     * 5. Handle multi-statement SQL properly
     * 6. Log the execution
     */
    @Override
    public void execute(Change change) {
        // TODO: Implement SQL execution
    }

    /**
     * Generate SQL (just return the SQL from SqlChange).
     *
     * Should:
     * 1. Cast Change to SqlChange
     * 2. Return the SQL string as-is
     */
    @Override
    public String generateSql(Change change) {
        // TODO: Implement SQL generation
        return null;
    }
}
