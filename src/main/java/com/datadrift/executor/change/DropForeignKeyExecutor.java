package com.datadrift.executor.change;

import com.datadrift.model.change.DropForeignKeyChange;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("dropForeignKey")
@RequiredArgsConstructor
public class DropForeignKeyExecutor implements ChangeExecutor<DropForeignKeyChange> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void execute(DropForeignKeyChange change) {
        String sql = generateSql(change);
        String qualifiedBaseTable = SqlEscapeUtil.qualifiedName(change.getBaseSchemaName(), change.getBaseTableName());

        log.info("Executing DROP FOREIGN KEY CONSTRAINT on table: {}", qualifiedBaseTable);
        log.debug("SQL statement: {}", sql);

        jdbcTemplate.execute(sql);

        log.info("Successfully dropped foreign key constraint: {} from table: {}",
                change.getConstraintName(), qualifiedBaseTable);
    }

    @Override
    public String generateSql(DropForeignKeyChange change) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ");

        // Base table
        String qualifiedBaseTable;
        if (change.getBaseSchemaName() != null && !change.getBaseSchemaName().isBlank()) {
            qualifiedBaseTable = SqlEscapeUtil.qualifiedName(change.getBaseSchemaName(), change.getBaseTableName());
        } else {
            qualifiedBaseTable = SqlEscapeUtil.escapeIdentifier(change.getBaseTableName());
        }
        sql.append(qualifiedBaseTable);

        sql.append(" DROP CONSTRAINT ");

        if (Boolean.TRUE.equals(change.getIfExists())) {
            sql.append("IF EXISTS ");
        }

        sql.append(SqlEscapeUtil.escapeIdentifier(change.getConstraintName()));

        if (Boolean.TRUE.equals(change.getCascade())) {
            sql.append(" CASCADE");
        }

        return sql.toString();
    }
}
