package com.datadrift.sql;

import com.datadrift.sql.dialect.SqlDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateTableSqlBuilderTest {

    @Mock
    private SqlDialect dialect;

    private CreateTableSqlBuilder builder;

    @BeforeEach
    void setUp() {
        lenient().when(dialect.supportsIfNotExists()).thenReturn(true);
        lenient().when(dialect.getIfNotExistsSyntax()).thenReturn("IF NOT EXISTS ");
        lenient().when(dialect.supportsTableComments()).thenReturn(true);
        lenient().when(dialect.getTableCommentSyntax(anyString(), anyString()))
                .thenAnswer(invocation -> "COMMENT ON TABLE " + invocation.getArgument(0) + " IS " + invocation.getArgument(1));
        lenient().when(dialect.getColumnCommentSyntax(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> "COMMENT ON COLUMN " + invocation.getArgument(0) + "." + invocation.getArgument(1) + " IS " + invocation.getArgument(2));

        builder = new CreateTableSqlBuilder(dialect);
    }

    @Test
    void testBuild_SimpleTable() {
        // Given
        String sql = builder
                .table("users")
                .addDefinition("\"id\" INTEGER PRIMARY KEY")
                .addDefinition("\"name\" VARCHAR(100)")
                .build();

        // Then
        assertNotNull(sql);
        assertTrue(sql.contains("CREATE TABLE"));
        assertTrue(sql.contains("\"users\""));
        assertTrue(sql.contains("\"id\" INTEGER PRIMARY KEY"));
        assertTrue(sql.contains("\"name\" VARCHAR(100)"));
    }

    @Test
    void testBuild_WithSchema() {
        // Given
        String sql = builder
                .schema("public")
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .build();

        // Then
        assertTrue(sql.contains("\"public\".\"users\""));
    }

    @Test
    void testBuild_WithIfNotExists() {
        // Given
        String sql = builder
                .table("users")
                .ifNotExists(true)
                .addDefinition("\"id\" INTEGER")
                .build();

        // Then
        assertTrue(sql.contains("IF NOT EXISTS"));
    }

    @Test
    void testBuild_WithoutIfNotExists() {
        // Given
        String sql = builder
                .table("users")
                .ifNotExists(false)
                .addDefinition("\"id\" INTEGER")
                .build();

        // Then
        assertFalse(sql.contains("IF NOT EXISTS"));
    }

    @Test
    void testBuild_MultipleDefinitions() {
        // Given
        String sql = builder
                .table("users")
                .addDefinition("\"id\" INTEGER PRIMARY KEY")
                .addDefinition("\"name\" VARCHAR(100)")
                .addDefinition("\"email\" VARCHAR(255) UNIQUE")
                .build();

        // Then
        assertTrue(sql.contains("\"id\" INTEGER PRIMARY KEY"));
        assertTrue(sql.contains("\"name\" VARCHAR(100)"));
        assertTrue(sql.contains("\"email\" VARCHAR(255) UNIQUE"));
        // Check that definitions are separated by commas
        assertTrue(sql.matches("(?s).*\"id\".*,.*\"name\".*,.*\"email\".*"));
    }

    @Test
    void testBuild_WithTableComment() {
        // Given
        String sql = builder
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .tableComment("User accounts")
                .build();

        // Then
        assertTrue(sql.contains("COMMENT ON TABLE"));
        assertTrue(sql.contains("'User accounts'"));
    }

    @Test
    void testBuild_WithColumnComment() {
        // Given
        String sql = builder
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .columnComment("id", "Primary key")
                .build();

        // Then
        assertTrue(sql.contains("COMMENT ON COLUMN"));
        assertTrue(sql.contains("\"id\""));
        assertTrue(sql.contains("'Primary key'"));
    }

    @Test
    void testBuild_WithMultipleComments() {
        // Given
        String sql = builder
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .addDefinition("\"name\" VARCHAR(100)")
                .tableComment("User accounts")
                .columnComment("id", "Primary key")
                .columnComment("name", "User full name")
                .build();

        // Then
        assertTrue(sql.contains("COMMENT ON TABLE"));
        assertTrue(sql.contains("COMMENT ON COLUMN"));
        assertTrue(sql.contains("'User accounts'"));
        assertTrue(sql.contains("'Primary key'"));
        assertTrue(sql.contains("'User full name'"));
    }

    @Test
    void testBuild_ThrowsException_WhenTableNameMissing() {
        // Given
        builder.addDefinition("\"id\" INTEGER");

        // Then
        assertThrows(IllegalStateException.class, () -> builder.build());
    }

    @Test
    void testBuild_ThrowsException_WhenNoDefinitions() {
        // Given
        builder.table("users");

        // Then
        assertThrows(IllegalStateException.class, () -> builder.build());
    }

    @Test
    void testBuild_IgnoresNullTableComment() {
        // Given
        String sql = builder
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .tableComment(null)
                .build();

        // Then
        assertFalse(sql.contains("COMMENT ON TABLE"));
    }

    @Test
    void testBuild_IgnoresBlankTableComment() {
        // Given
        String sql = builder
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .tableComment("   ")
                .build();

        // Then
        assertFalse(sql.contains("COMMENT ON TABLE"));
    }

    @Test
    void testBuild_IgnoresNullColumnComment() {
        // Given
        String sql = builder
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .columnComment("id", null)
                .build();

        // Then
        assertFalse(sql.contains("COMMENT ON COLUMN"));
    }

    @Test
    void testBuildStatements_ReturnsListOfStatements() {
        // Given
        builder
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .tableComment("User accounts");

        // When
        List<String> statements = builder.buildStatements();

        // Then
        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("CREATE TABLE"));
        assertTrue(statements.get(1).contains("COMMENT ON TABLE"));
    }

    @Test
    void testBuildStatements_WithoutComments() {
        // Given
        builder
                .table("users")
                .addDefinition("\"id\" INTEGER");

        // When
        List<String> statements = builder.buildStatements();

        // Then
        assertEquals(1, statements.size());
        assertTrue(statements.get(0).contains("CREATE TABLE"));
    }

    @Test
    void testBuildStatements_WithMultipleComments() {
        // Given
        builder
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .tableComment("User accounts")
                .columnComment("id", "Primary key");

        // When
        List<String> statements = builder.buildStatements();

        // Then
        assertEquals(3, statements.size());
        assertTrue(statements.get(0).contains("CREATE TABLE"));
        assertTrue(statements.get(1).contains("COMMENT ON TABLE"));
        assertTrue(statements.get(2).contains("COMMENT ON COLUMN"));
    }

    @Test
    void testFluentApi_AllMethods() {
        // Test that all methods return the builder for chaining
        CreateTableSqlBuilder result = builder
                .table("users")
                .schema("public")
                .ifNotExists(true)
                .addDefinition("\"id\" INTEGER")
                .tableComment("Users")
                .columnComment("id", "ID");

        assertSame(builder, result);
    }

    @Test
    void testBuild_ComplexTable() {
        // Given
        String sql = builder
                .schema("sales")
                .table("orders")
                .ifNotExists(true)
                .addDefinition("\"id\" INTEGER PRIMARY KEY")
                .addDefinition("\"user_id\" INTEGER NOT NULL")
                .addDefinition("\"total\" DECIMAL(10,2)")
                .addDefinition("FOREIGN KEY (\"user_id\") REFERENCES users(id)")
                .tableComment("Customer orders")
                .columnComment("id", "Order ID")
                .columnComment("user_id", "Reference to user")
                .build();

        // Then
        assertTrue(sql.contains("\"sales\".\"orders\""));
        assertTrue(sql.contains("IF NOT EXISTS"));
        assertTrue(sql.contains("\"id\" INTEGER PRIMARY KEY"));
        assertTrue(sql.contains("FOREIGN KEY"));
        assertTrue(sql.contains("COMMENT ON TABLE"));
        assertTrue(sql.contains("COMMENT ON COLUMN"));
    }

    @Test
    void testBuild_WhenDialectDoesNotSupportIfNotExists() {
        // Given
        when(dialect.supportsIfNotExists()).thenReturn(false);

        String sql = builder
                .table("users")
                .ifNotExists(true)
                .addDefinition("\"id\" INTEGER")
                .build();

        // Then
        assertFalse(sql.contains("IF NOT EXISTS"));
    }

    @Test
    void testBuild_WhenDialectDoesNotSupportComments() {
        // Given
        when(dialect.supportsTableComments()).thenReturn(false);

        String sql = builder
                .table("users")
                .addDefinition("\"id\" INTEGER")
                .tableComment("User accounts")
                .build();

        // Then
        assertFalse(sql.contains("COMMENT ON TABLE"));
    }
}
