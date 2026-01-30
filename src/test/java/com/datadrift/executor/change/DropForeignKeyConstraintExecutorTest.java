package com.datadrift.executor.change;

import com.datadrift.model.change.DropForeignKeyConstraintChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DropForeignKeyConstraintExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DropForeignKeyConstraintExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DropForeignKeyConstraintExecutor(jdbcTemplate);
    }

    @Test
    void testExecute_SimpleDropForeignKey() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testGenerateSql_SimpleDropForeignKey() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertEquals("ALTER TABLE \"orders\" DROP CONSTRAINT \"fk_orders_users\"", sql);
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseSchemaName("public");
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("ALTER TABLE \"public\".\"orders\" DROP CONSTRAINT \"fk_orders_users\"", sql);
    }

    @Test
    void testGenerateSql_WithIfExists() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setIfExists(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("ALTER TABLE \"orders\" DROP CONSTRAINT IF EXISTS \"fk_orders_users\"", sql);
    }

    @Test
    void testGenerateSql_WithCascade() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("ALTER TABLE \"orders\" DROP CONSTRAINT \"fk_orders_users\" CASCADE", sql);
    }

    @Test
    void testGenerateSql_WithIfExistsAndCascade() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setIfExists(true);
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("ALTER TABLE \"orders\" DROP CONSTRAINT IF EXISTS \"fk_orders_users\" CASCADE", sql);
    }

    @Test
    void testGenerateSql_WithSchemaAndAllOptions() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseSchemaName("public");
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setIfExists(true);
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("ALTER TABLE \"public\".\"orders\" DROP CONSTRAINT IF EXISTS \"fk_orders_users\" CASCADE", sql);
    }

    @Test
    void testGenerateSql_WithCascadeFalse() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setCascade(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("ALTER TABLE \"orders\" DROP CONSTRAINT \"fk_orders_users\"", sql);
        assertFalse(sql.contains("CASCADE"));
    }

    @Test
    void testGenerateSql_WithIfExistsFalse() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setIfExists(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("ALTER TABLE \"orders\" DROP CONSTRAINT \"fk_orders_users\"", sql);
        assertFalse(sql.contains("IF EXISTS"));
    }

    @Test
    void testGenerateSql_EscapesTableName() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("order_table");
        change.setConstraintName("fk_custom");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"order_table\""));
    }

    @Test
    void testGenerateSql_EscapesSchemaAndTableName() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseSchemaName("my_schema");
        change.setBaseTableName("my_table");
        change.setConstraintName("my_constraint");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"my_schema\".\"my_table\""));
        assertTrue(sql.contains("\"my_constraint\""));
    }

    @Test
    void testGenerateSql_EscapesConstraintName() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_constraint_name");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"fk_constraint_name\""));
    }
}
