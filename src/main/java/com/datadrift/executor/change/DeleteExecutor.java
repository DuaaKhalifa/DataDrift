package com.datadrift.executor.change;

import com.datadrift.model.change.DeleteChange;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("delete")
@RequiredArgsConstructor
public class DeleteExecutor implements ChangeExecutor<DeleteChange> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void execute(DeleteChange change) {
        String sql = generateSql(change);
        String qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());

        log.info("Executing DELETE on table: {}", qualifiedTableName);
        log.debug("SQL statement: {}", sql);

        jdbcTemplate.execute(sql);

        log.info("Successfully executed DELETE on table: {}", qualifiedTableName);
    }

    @Override
    public String generateSql(DeleteChange change) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");

        // Table name
        String qualifiedTableName;
        if (change.getSchemaName() != null && !change.getSchemaName().isBlank()) {
            qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());
        } else {
            qualifiedTableName = SqlEscapeUtil.escapeIdentifier(change.getTableName());
        }
        sql.append(qualifiedTableName);

        // WHERE clause
        sql.append(" WHERE ");
        // Validate the WHERE clause to prevent SQL injection
        SqlEscapeUtil.validateExpression(change.getWhere());
        sql.append(change.getWhere());

        return sql.toString();
    }
}
