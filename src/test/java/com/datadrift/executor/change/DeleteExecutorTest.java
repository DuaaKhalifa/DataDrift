package com.datadrift.executor.change;

import com.datadrift.model.change.DeleteChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DeleteExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DeleteExecutor(jdbcTemplate);
    }

    @Test
    void testExecute_SimpleDelete() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testGenerateSql_SimpleDelete() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertEquals("DELETE FROM \"users\" WHERE id = 1", sql);
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setSchemaName("public");
        change.setTableName("users");
        change.setWhere("id = 1");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DELETE FROM \"public\".\"users\" WHERE id = 1", sql);
    }

    @Test
    void testGenerateSql_ComplexWhereClause() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("status = 'inactive' AND created_at < '2020-01-01'");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE status = 'inactive' AND created_at < '2020-01-01'"));
    }

    @Test
    void testGenerateSql_WhereWithMultipleConditions() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("orders");
        change.setWhere("total = 0 OR status = 'cancelled'");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("WHERE total = 0 OR status = 'cancelled'"));
    }

    @Test
    void testGenerateSql_WhereWithInClause() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("id IN (1, 2, 3)");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("WHERE id IN (1, 2, 3)"));
    }

    @Test
    void testGenerateSql_CompleteExample() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setSchemaName("public");
        change.setTableName("logs");
        change.setWhere("created_at < '2020-01-01' AND level = 'DEBUG'");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DELETE FROM \"public\".\"logs\" WHERE created_at < '2020-01-01' AND level = 'DEBUG'", sql);
    }

    @Test
    void testGenerateSql_EscapesTableName() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("user_table");
        change.setWhere("id = 1");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"user_table\""));
    }

    @Test
    void testGenerateSql_EscapesSchemaAndTableName() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setSchemaName("my_schema");
        change.setTableName("my_table");
        change.setWhere("id = 1");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"my_schema\".\"my_table\""));
    }

    @Test
    void testGenerateSql_ThrowsException_WhenWhereCauseSqlInjection() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("id = 1; DROP TABLE users;--");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.generateSql(change)
        );
        assertTrue(exception.getMessage().contains("Invalid SQL expression"));
    }

    @Test
    void testGenerateSql_ThrowsException_WhenWhereContainsSqlComment() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("id = 1 -- comment");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.generateSql(change)
        );
        assertTrue(exception.getMessage().contains("Invalid SQL expression"));
    }

    @Test
    void testGenerateSql_ThrowsException_WhenWhereContainsBlockComment() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("id = 1 /* comment */");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.generateSql(change)
        );
        assertTrue(exception.getMessage().contains("Invalid SQL expression"));
    }
}
