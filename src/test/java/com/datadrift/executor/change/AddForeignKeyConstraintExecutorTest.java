package com.datadrift.executor.change;

import com.datadrift.model.change.AddForeignKeyConstraintChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddForeignKeyConstraintExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AddForeignKeyConstraintExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new AddForeignKeyConstraintExecutor(jdbcTemplate);
    }

    @Test
    void testExecute_SimpleForeignKey() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testGenerateSql_SimpleForeignKey() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertTrue(sql.contains("ALTER TABLE \"orders\""));
        assertTrue(sql.contains("ADD CONSTRAINT"));
        assertTrue(sql.contains("FOREIGN KEY (\"user_id\")"));
        assertTrue(sql.contains("REFERENCES \"users\" (\"id\")"));
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setBaseSchemaName("public");
        change.setReferencedSchemaName("public");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ALTER TABLE \"public\".\"orders\""));
        assertTrue(sql.contains("REFERENCES \"public\".\"users\""));
    }

    @Test
    void testGenerateSql_WithCustomConstraintName() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setConstraintName("fk_custom_name");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ADD CONSTRAINT \"fk_custom_name\""));
    }

    @Test
    void testGenerateSql_AutoGeneratesConstraintName() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setConstraintName(null);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ADD CONSTRAINT \"fk_orders_users\""));
    }

    @Test
    void testGenerateSql_WithOnDelete() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setOnDelete("CASCADE");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ON DELETE CASCADE"));
    }

    @Test
    void testGenerateSql_WithOnUpdate() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setOnUpdate("RESTRICT");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ON UPDATE RESTRICT"));
    }

    @Test
    void testGenerateSql_WithOnDeleteAndOnUpdate() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setOnDelete("CASCADE");
        change.setOnUpdate("RESTRICT");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ON DELETE CASCADE"));
        assertTrue(sql.contains("ON UPDATE RESTRICT"));
    }

    @Test
    void testGenerateSql_WithDeferrable() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setDeferrable(true);
        change.setInitiallyDeferred(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("DEFERRABLE INITIALLY IMMEDIATE"));
    }

    @Test
    void testGenerateSql_WithDeferrableInitiallyDeferred() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setDeferrable(true);
        change.setInitiallyDeferred(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("DEFERRABLE INITIALLY DEFERRED"));
    }

    @Test
    void testGenerateSql_WithoutDeferrable() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setDeferrable(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertFalse(sql.contains("DEFERRABLE"));
    }

    @Test
    void testGenerateSql_MultipleColumns() {
        // Given
        AddForeignKeyConstraintChange change = new AddForeignKeyConstraintChange();
        change.setBaseTableName("order_items");
        change.setBaseColumnNames(List.of("order_id", "product_id"));
        change.setReferencedTableName("orders");
        change.setReferencedColumnNames(List.of("id", "product_id"));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("FOREIGN KEY (\"order_id\", \"product_id\")"));
        assertTrue(sql.contains("REFERENCES \"orders\" (\"id\", \"product_id\")"));
    }

    @Test
    void testGenerateSql_CompleteExample() {
        // Given
        AddForeignKeyConstraintChange change = new AddForeignKeyConstraintChange();
        change.setBaseSchemaName("public");
        change.setBaseTableName("orders");
        change.setBaseColumnNames(List.of("user_id"));
        change.setReferencedSchemaName("public");
        change.setReferencedTableName("users");
        change.setReferencedColumnNames(List.of("id"));
        change.setConstraintName("fk_orders_users");
        change.setOnDelete("CASCADE");
        change.setOnUpdate("RESTRICT");
        change.setDeferrable(true);
        change.setInitiallyDeferred(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ALTER TABLE \"public\".\"orders\""));
        assertTrue(sql.contains("ADD CONSTRAINT \"fk_orders_users\""));
        assertTrue(sql.contains("FOREIGN KEY (\"user_id\")"));
        assertTrue(sql.contains("REFERENCES \"public\".\"users\" (\"id\")"));
        assertTrue(sql.contains("ON DELETE CASCADE"));
        assertTrue(sql.contains("ON UPDATE RESTRICT"));
        assertTrue(sql.contains("DEFERRABLE INITIALLY IMMEDIATE"));
    }

    @Test
    void testGenerateSql_EscapesIdentifiers() {
        // Given
        AddForeignKeyConstraintChange change = new AddForeignKeyConstraintChange();
        change.setBaseTableName("order_table");
        change.setBaseColumnNames(List.of("user_id_column"));
        change.setReferencedTableName("user_table");
        change.setReferencedColumnNames(List.of("id_column"));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"order_table\""));
        assertTrue(sql.contains("\"user_id_column\""));
        assertTrue(sql.contains("\"user_table\""));
        assertTrue(sql.contains("\"id_column\""));
    }

    @Test
    void testGenerateSql_OnActionsCaseInsensitive() {
        // Given
        AddForeignKeyConstraintChange change = createSimpleChange();
        change.setOnDelete("cascade");
        change.setOnUpdate("restrict");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ON DELETE CASCADE"));
        assertTrue(sql.contains("ON UPDATE RESTRICT"));
    }

    @Test
    void testGenerateSql_AllOnActions() {
        // Test all valid ON DELETE/UPDATE actions
        String[] actions = {"CASCADE", "RESTRICT", "SET NULL", "SET DEFAULT", "NO ACTION"};

        for (String action : actions) {
            // Given
            AddForeignKeyConstraintChange change = createSimpleChange();
            change.setOnDelete(action);

            // When
            String sql = executor.generateSql(change);

            // Then
            assertTrue(sql.contains("ON DELETE " + action.toUpperCase()),
                    "Should contain: ON DELETE " + action);
        }
    }

    private AddForeignKeyConstraintChange createSimpleChange() {
        AddForeignKeyConstraintChange change = new AddForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setBaseColumnNames(List.of("user_id"));
        change.setReferencedTableName("users");
        change.setReferencedColumnNames(List.of("id"));
        return change;
    }
}
