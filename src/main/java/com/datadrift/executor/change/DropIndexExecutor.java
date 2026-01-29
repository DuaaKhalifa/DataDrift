package com.datadrift.executor.change;

import com.datadrift.model.change.DropIndexChange;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("dropIndex")
@RequiredArgsConstructor
public class DropIndexExecutor implements ChangeExecutor<DropIndexChange> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void execute(DropIndexChange change) {
        String sql = generateSql(change);
        String qualifiedIndexName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getIndexName());

        log.info("Executing DROP INDEX on index: {}", qualifiedIndexName);
        log.debug("SQL statement: {}", sql);

        jdbcTemplate.execute(sql);

        log.info("Successfully dropped index: {}", qualifiedIndexName);
    }

    @Override
    public String generateSql(DropIndexChange change) {
        StringBuilder sql = new StringBuilder();
        sql.append("DROP INDEX ");

        if (Boolean.TRUE.equals(change.getIfExists())) {
            sql.append("IF EXISTS ");
        }

        String qualifiedIndexName;
        if (change.getSchemaName() != null && !change.getSchemaName().isBlank()) {
            qualifiedIndexName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getIndexName());
        } else {
            qualifiedIndexName = SqlEscapeUtil.escapeIdentifier(change.getIndexName());
        }

        sql.append(qualifiedIndexName);

        if (Boolean.TRUE.equals(change.getCascade())) {
            sql.append(" CASCADE");
        }

        return sql.toString();
    }
}
