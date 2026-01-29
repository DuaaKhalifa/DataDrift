package com.datadrift.executor.change;

import com.datadrift.model.change.CreateIndexChange;
import com.datadrift.sql.dialect.SqlDialect;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component("createIndex")
@RequiredArgsConstructor
public class CreateIndexExecutor implements ChangeExecutor<CreateIndexChange> {

    private final JdbcTemplate jdbcTemplate;
    private final SqlDialect sqlDialect;

    @Override
    public void execute(CreateIndexChange change) {
        String sql = generateSql(change);
        String qualifiedTableName = SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName());

        log.info("Executing CREATE INDEX on table: {}", qualifiedTableName);
        log.debug("SQL: {}", sql);

        jdbcTemplate.execute(sql);

        log.info("Successfully created index: {}", getIndexName(change));
    }

    @Override
    public String generateSql(CreateIndexChange change) {
        StringBuilder sql = new StringBuilder();

        // CREATE [UNIQUE] INDEX
        sql.append("CREATE ");
        if (Boolean.TRUE.equals(change.getUnique())) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ");

        // Index name
        String indexName = getIndexName(change);
        sql.append(SqlEscapeUtil.escapeIdentifier(indexName));

        // ON table
        sql.append(" ON ");
        if (change.getSchemaName() != null && !change.getSchemaName().isBlank()) {
            sql.append(SqlEscapeUtil.qualifiedName(change.getSchemaName(), change.getTableName()));
        } else {
            sql.append(SqlEscapeUtil.escapeIdentifier(change.getTableName()));
        }

        // Columns
        sql.append(" (");
        String columns = change.getColumns().stream()
                .map(SqlEscapeUtil::escapeIdentifier)
                .collect(Collectors.joining(", "));
        sql.append(columns);
        sql.append(")");

        return sql.toString();
    }

    private String getIndexName(CreateIndexChange change) {
        if (change.getIndexName() != null && !change.getIndexName().isBlank()) {
            return change.getIndexName();
        }

        // Auto-generate index name: idx_tablename_column1_column2
        String prefix = Boolean.TRUE.equals(change.getUnique()) ? "uniq" : "idx";
        String columnsJoined = String.join("_", change.getColumns());
        return prefix + "_" + change.getTableName() + "_" + columnsJoined;
    }
}
