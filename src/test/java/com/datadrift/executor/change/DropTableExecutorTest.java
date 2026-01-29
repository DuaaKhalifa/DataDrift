package com.datadrift.executor.change;

import com.datadrift.model.change.DropTableChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DropTableExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DropTableExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DropTableExecutor(jdbcTemplate);
    }

    @Test
    void testExecute_SimpleDropTable() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testGenerateSql_SimpleDropTable() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertEquals("DROP TABLE \"users\"", sql);
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setSchemaName("public");
        change.setTableName("users");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP TABLE \"public\".\"users\"", sql);
    }

    @Test
    void testGenerateSql_WithIfExists() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");
        change.setIfExists(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP TABLE IF EXISTS \"users\"", sql);
    }

    @Test
    void testGenerateSql_WithCascade() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP TABLE \"users\" CASCADE", sql);
    }

    @Test
    void testGenerateSql_WithIfExistsAndCascade() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");
        change.setIfExists(true);
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP TABLE IF EXISTS \"users\" CASCADE", sql);
    }

    @Test
    void testGenerateSql_WithSchemaAndAllOptions() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setSchemaName("public");
        change.setTableName("users");
        change.setIfExists(true);
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP TABLE IF EXISTS \"public\".\"users\" CASCADE", sql);
    }

    @Test
    void testGenerateSql_WithCascadeFalse() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");
        change.setCascade(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP TABLE \"users\"", sql);
        assertFalse(sql.contains("CASCADE"));
    }

    @Test
    void testGenerateSql_WithIfExistsFalse() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");
        change.setIfExists(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP TABLE \"users\"", sql);
        assertFalse(sql.contains("IF EXISTS"));
    }

    @Test
    void testGenerateSql_EscapesTableName() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("user_table");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"user_table\""));
    }

    @Test
    void testGenerateSql_EscapesSchemaAndTableName() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setSchemaName("my_schema");
        change.setTableName("my_table");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"my_schema\".\"my_table\""));
    }
}
