package com.datadrift.executor.change;

import com.datadrift.model.change.AddForeignKeyConstraintChange;
import com.datadrift.util.SqlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Controls when a constraint is validated during a transaction.
 *
 * | Option                        | When Checked             | Can Change? |
 * |-------------------------------|--------------------------|-------------|
 * | NOT_DEFERRABLE                | After each statement     | No          |
 * | DEFERRABLE_INITIALLY_IMMEDIATE| After each statement     | Yes         |
 * | DEFERRABLE_INITIALLY_DEFERRED | At COMMIT                | Yes         |
 *
 * Deferred constraints are useful for:
 * - Circular references (table A references B, B references A)
 * - Self-referencing tables (employee → manager)
 * - Bulk data loading where order of inserts is inconvenient
 *
 * Example:
 *   BEGIN;
 *     INSERT INTO orders (user_id) VALUES (1);  -- user doesn't exist yet
 *     INSERT INTO users (id) VALUES (1);        -- now it does
 *   COMMIT;  -- constraint checked here, passes
 */
@Slf4j
@Component("addForeignKeyConstraint")
@RequiredArgsConstructor
public class AddForeignKeyConstraintExecutor implements ChangeExecutor<AddForeignKeyConstraintChange> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void execute(AddForeignKeyConstraintChange change) {
        String sql = generateSql(change);
        String qualifiedBaseTable = SqlEscapeUtil.qualifiedName(change.getBaseSchemaName(), change.getBaseTableName());

        log.info("Executing ADD FOREIGN KEY CONSTRAINT on table: {}", qualifiedBaseTable);
        log.debug("SQL statement: {}", sql);

        jdbcTemplate.execute(sql);

        log.info("Successfully added foreign key constraint to table: {}", qualifiedBaseTable);
    }

    @Override
    public String generateSql(AddForeignKeyConstraintChange change) {
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

        sql.append(" ADD CONSTRAINT ");

        // Constraint name (auto-generate if not provided)
        String constraintName = change.getConstraintName();
        if (constraintName == null || constraintName.isBlank()) {
            constraintName = generateConstraintName(change);
        }
        sql.append(SqlEscapeUtil.escapeIdentifier(constraintName));

        sql.append(" FOREIGN KEY (");

        // Base columns
        String baseColumns = change.getBaseColumnNames().stream()
                .map(SqlEscapeUtil::escapeIdentifier)
                .collect(Collectors.joining(", "));
        sql.append(baseColumns);

        sql.append(") REFERENCES ");

        // Referenced table
        String qualifiedReferencedTable;
        if (change.getReferencedSchemaName() != null && !change.getReferencedSchemaName().isBlank()) {
            qualifiedReferencedTable = SqlEscapeUtil.qualifiedName(
                    change.getReferencedSchemaName(),
                    change.getReferencedTableName()
            );
        } else {
            qualifiedReferencedTable = SqlEscapeUtil.escapeIdentifier(change.getReferencedTableName());
        }
        sql.append(qualifiedReferencedTable);

        sql.append(" (");

        // Referenced columns
        String referencedColumns = change.getReferencedColumnNames().stream()
                .map(SqlEscapeUtil::escapeIdentifier)
                .collect(Collectors.joining(", "));
        sql.append(referencedColumns);

        sql.append(")");

        // ON DELETE
        if (change.getOnDelete() != null && !change.getOnDelete().isBlank()) {
            sql.append(" ON DELETE ").append(change.getOnDelete().toUpperCase());
        }

        // ON UPDATE
        if (change.getOnUpdate() != null && !change.getOnUpdate().isBlank()) {
            sql.append(" ON UPDATE ").append(change.getOnUpdate().toUpperCase());
        }

        // DEFERRABLE
        if (Boolean.TRUE.equals(change.getDeferrable())) {
            sql.append(" DEFERRABLE");

            if (Boolean.TRUE.equals(change.getInitiallyDeferred())) {
                sql.append(" INITIALLY DEFERRED");
            } else {
                sql.append(" INITIALLY IMMEDIATE");
            }
        }

        return sql.toString();
    }

    private String generateConstraintName(AddForeignKeyConstraintChange change) {
        // Generate name like: fk_basetable_reftable
        String baseTable = change.getBaseTableName().toLowerCase();
        String refTable = change.getReferencedTableName().toLowerCase();

        // Truncate if too long (PostgreSQL limit is 63 characters)
        String name = "fk_" + baseTable + "_" + refTable;
        if (name.length() > 63) {
            name = name.substring(0, 63);
        }

        return name;
    }
}
