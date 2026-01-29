package com.datadrift.executor.change;

import com.datadrift.model.change.DropIndexChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DropIndexExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DropIndexExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DropIndexExecutor(jdbcTemplate);
    }

    @Test
    void testExecute_SimpleDropIndex() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testGenerateSql_SimpleDropIndex() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertEquals("DROP INDEX \"idx_users_email\"", sql);
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setSchemaName("public");
        change.setIndexName("idx_users_email");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP INDEX \"public\".\"idx_users_email\"", sql);
    }

    @Test
    void testGenerateSql_WithIfExists() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");
        change.setIfExists(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP INDEX IF EXISTS \"idx_users_email\"", sql);
    }

    @Test
    void testGenerateSql_WithCascade() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP INDEX \"idx_users_email\" CASCADE", sql);
    }

    @Test
    void testGenerateSql_WithIfExistsAndCascade() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");
        change.setIfExists(true);
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP INDEX IF EXISTS \"idx_users_email\" CASCADE", sql);
    }

    @Test
    void testGenerateSql_WithSchemaAndAllOptions() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setSchemaName("public");
        change.setIndexName("idx_users_email");
        change.setIfExists(true);
        change.setCascade(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP INDEX IF EXISTS \"public\".\"idx_users_email\" CASCADE", sql);
    }

    @Test
    void testGenerateSql_WithCascadeFalse() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");
        change.setCascade(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP INDEX \"idx_users_email\"", sql);
        assertFalse(sql.contains("CASCADE"));
    }

    @Test
    void testGenerateSql_WithIfExistsFalse() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");
        change.setIfExists(false);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("DROP INDEX \"idx_users_email\"", sql);
        assertFalse(sql.contains("IF EXISTS"));
    }

    @Test
    void testGenerateSql_EscapesIndexName() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_user_email");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"idx_user_email\""));
    }

    @Test
    void testGenerateSql_EscapesSchemaAndIndexName() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setSchemaName("my_schema");
        change.setIndexName("my_index");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"my_schema\".\"my_index\""));
    }
}
