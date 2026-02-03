package com.datadrift.executor.change;

import com.datadrift.model.change.SqlChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqlExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    private SqlExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new SqlExecutor(jdbcTemplate);
    }

    @Test
    void testExecute_SimpleSql() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("SELECT * FROM users");

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute("SELECT * FROM users");
    }

    @Test
    void testExecute_ComplexSql() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("CREATE OR REPLACE FUNCTION test() RETURNS void AS $$ BEGIN END; $$ LANGUAGE plpgsql");

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testExecute_MultiLineSql() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("CREATE TABLE test (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    name VARCHAR(100)\n" +
                ")");

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testExecute_WithSplitStatements() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("INSERT INTO users (name) VALUES ('John'); INSERT INTO users (name) VALUES ('Jane')");
        change.setSplitStatements(true);

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(2)).execute(sqlCaptor.capture());
        List<String> executedSql = sqlCaptor.getAllValues();
        assertEquals("INSERT INTO users (name) VALUES ('John')", executedSql.get(0));
        assertEquals("INSERT INTO users (name) VALUES ('Jane')", executedSql.get(1));
    }

    @Test
    void testExecute_WithSplitStatementsFalse() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("INSERT INTO users (name) VALUES ('John'); INSERT INTO users (name) VALUES ('Jane')");
        change.setSplitStatements(false);

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testExecute_WithCustomDelimiter() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("INSERT INTO users (name) VALUES ('John')|| INSERT INTO users (name) VALUES ('Jane')");
        change.setSplitStatements(true);
        change.setEndDelimiter("||");

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(2)).execute(sqlCaptor.capture());
        List<String> executedSql = sqlCaptor.getAllValues();
        assertEquals("INSERT INTO users (name) VALUES ('John')", executedSql.get(0));
        assertEquals("INSERT INTO users (name) VALUES ('Jane')", executedSql.get(1));
    }

    @Test
    void testExecute_WithThreeStatements() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("CREATE TABLE t1 (id INT); CREATE TABLE t2 (id INT); CREATE TABLE t3 (id INT)");
        change.setSplitStatements(true);

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(3)).execute(sqlCaptor.capture());
        List<String> executedSql = sqlCaptor.getAllValues();
        assertEquals("CREATE TABLE t1 (id INT)", executedSql.get(0));
        assertEquals("CREATE TABLE t2 (id INT)", executedSql.get(1));
        assertEquals("CREATE TABLE t3 (id INT)", executedSql.get(2));
    }

    @Test
    void testExecute_IgnoresEmptyStatements() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("INSERT INTO users (name) VALUES ('John');;; INSERT INTO users (name) VALUES ('Jane')");
        change.setSplitStatements(true);

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(2)).execute(sqlCaptor.capture());
    }

    @Test
    void testExecute_TrimsWhitespace() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("  INSERT INTO users (name) VALUES ('John')  ;   INSERT INTO users (name) VALUES ('Jane')  ");
        change.setSplitStatements(true);

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(2)).execute(sqlCaptor.capture());
        List<String> executedSql = sqlCaptor.getAllValues();
        assertEquals("INSERT INTO users (name) VALUES ('John')", executedSql.get(0));
        assertEquals("INSERT INTO users (name) VALUES ('Jane')", executedSql.get(1));
    }

    @Test
    void testGenerateSql_ReturnsSqlAsIs() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("SELECT * FROM users WHERE id = 1");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("SELECT * FROM users WHERE id = 1", sql);
    }

    @Test
    void testGenerateSql_WithSplitStatements() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("INSERT INTO users (name) VALUES ('John'); INSERT INTO users (name) VALUES ('Jane')");
        change.setSplitStatements(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals("INSERT INTO users (name) VALUES ('John'); INSERT INTO users (name) VALUES ('Jane')", sql);
    }

    @Test
    void testGenerateSql_ComplexSqlWithFunctions() {
        // Given
        SqlChange change = new SqlChange();
        String complexSql = "CREATE OR REPLACE FUNCTION update_timestamp()\n" +
                "RETURNS TRIGGER AS $$\n" +
                "BEGIN\n" +
                "    NEW.updated_at = CURRENT_TIMESTAMP;\n" +
                "    RETURN NEW;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql";
        change.setSql(complexSql);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertEquals(complexSql, sql);
    }

    @Test
    void testExecute_AllowsSemicolonsInSql() {
        // Given - SQL with semicolons that would be blocked by WHERE validation
        SqlChange change = new SqlChange();
        change.setSql("DROP TABLE IF EXISTS users;");

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> executor.execute(change));
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testExecute_AllowsCommentsInSql() {
        // Given - SQL with comments that would be blocked by WHERE validation
        SqlChange change = new SqlChange();
        change.setSql("-- This is a comment\nSELECT * FROM users");

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> executor.execute(change));
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testExecute_AllowsBlockCommentsInSql() {
        // Given - SQL with block comments
        SqlChange change = new SqlChange();
        change.setSql("SELECT * FROM users /* inline comment */");

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> executor.execute(change));
        verify(jdbcTemplate, times(1)).execute(anyString());
    }
}
