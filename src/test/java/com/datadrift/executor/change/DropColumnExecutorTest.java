package com.datadrift.executor.change;

import com.datadrift.model.change.DropColumnChange;
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
class DropColumnExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DropColumnExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DropColumnExecutor(jdbcTemplate);
    }

    @Test
    void testExecute_SingleColumn() {
        // Given
        DropColumnChange change = createSimpleDropColumnChange();

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testGenerateSql_SingleColumn() {
        // Given
        DropColumnChange change = createSimpleDropColumnChange();

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertTrue(sql.contains("ALTER TABLE"));
        assertTrue(sql.contains("\"users\""));
        assertTrue(sql.contains("DROP COLUMN"));
        assertTrue(sql.contains("\"role\""));
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        DropColumnChange change = createSimpleDropColumnChange();
        change.setSchemaName("public");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"public\".\"users\""));
    }

    @Test
    void testGenerateSql_WithIfExists() {
        // Given
        DropColumnChange change = createSimpleDropColumnChange();
        change.setIfExists(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("DROP COLUMN IF EXISTS"));
    }

    @Test
    void testGenerateSql_WithCascade() {
        // Given
        DropColumnChange change = createSimpleDropColumnChange();
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("CASCADE"));
    }

    @Test
    void testGenerateSql_WithIfExistsAndCascade() {
        // Given
        DropColumnChange change = createSimpleDropColumnChange();
        change.setIfExists(true);
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("IF EXISTS"));
        assertTrue(sql.contains("CASCADE"));
    }

    @Test
    void testGenerateSql_MultipleColumns() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setTableName("users");
        change.setColumns(List.of("role", "status"));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"role\""));
        assertTrue(sql.contains("\"status\""));
        // Should have separate statements
        assertTrue(sql.contains(";"));
    }

    @Test
    void testExecute_MultipleColumns_ExecutesMultipleStatements() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setTableName("users");
        change.setColumns(List.of("role", "status"));

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(2)).execute(anyString());
    }

    @Test
    void testGenerateSql_CompleteExample() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setSchemaName("public");
        change.setTableName("users");
        change.setColumns(List.of("role"));
        change.setIfExists(true);
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ALTER TABLE \"public\".\"users\""));
        assertTrue(sql.contains("DROP COLUMN IF EXISTS"));
        assertTrue(sql.contains("\"role\""));
        assertTrue(sql.contains("CASCADE"));
    }

    @Test
    void testGenerateSql_WithCascadeFalse() {
        // Given
        DropColumnChange change = createSimpleDropColumnChange();
        change.setCascade(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertFalse(sql.contains("CASCADE"));
    }

    private DropColumnChange createSimpleDropColumnChange() {
        DropColumnChange change = new DropColumnChange();
        change.setTableName("users");
        change.setColumns(List.of("role"));
        return change;
    }
}
