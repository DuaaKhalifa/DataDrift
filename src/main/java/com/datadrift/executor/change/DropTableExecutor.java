package com.datadrift.executor.change;

import com.datadrift.model.change.DropTableChange;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("dropTable")
@RequiredArgsConstructor
public class DropTableExecutor implements ChangeExecutor<DropTableChange> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void execute(DropTableChange change) {
        String sql = generateSql(change);
        String qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());

        log.info("Executing DROP TABLE on table: {}", qualifiedTableName);
        log.debug("SQL statement: {}", sql);

        jdbcTemplate.execute(sql);

        log.info("Successfully dropped table: {}", qualifiedTableName);
    }

    @Override
    public String generateSql(DropTableChange change) {
        StringBuilder sql = new StringBuilder();
        sql.append("DROP TABLE ");

        if (Boolean.TRUE.equals(change.getIfExists())) {
            sql.append("IF EXISTS ");
        }

        String qualifiedTableName;
        if (change.getSchemaName() != null && !change.getSchemaName().isBlank()) {
            qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());
        } else {
            qualifiedTableName = SqlEscapeUtil.escapeIdentifier(change.getTableName());
        }

        sql.append(qualifiedTableName);

        if (Boolean.TRUE.equals(change.getCascade())) {
            sql.append(" CASCADE");
        }

        return sql.toString();
    }
}
