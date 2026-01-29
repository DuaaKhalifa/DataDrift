package com.datadrift.executor.change;

import com.datadrift.model.change.AddColumnChange;
import com.datadrift.model.change.CreateTableChange.ColumnConfig;
import com.datadrift.model.change.CreateTableChange.ConstraintsConfig;
import com.datadrift.sql.dialect.SqlDialect;
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
class AddColumnExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SqlDialect sqlDialect;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    private AddColumnExecutor executor;

    @BeforeEach
    void setUp() {
        lenient().when(sqlDialect.getAutoIncrementSyntax()).thenReturn("GENERATED ALWAYS AS IDENTITY");
        lenient().when(sqlDialect.getGeneratedColumnSyntax(anyString(), anyBoolean()))
                .thenAnswer(invocation -> {
                    String expr = invocation.getArgument(0);
                    return "GENERATED ALWAYS AS (" + expr + ") STORED";
                });

        executor = new AddColumnExecutor(jdbcTemplate, sqlDialect);
    }

    @Test
    void testExecute_SingleColumn() {
        // Given
        AddColumnChange change = createSimpleAddColumnChange();

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testGenerateSql_SingleColumn() {
        // Given
        AddColumnChange change = createSimpleAddColumnChange();

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertTrue(sql.contains("ALTER TABLE"));
        assertTrue(sql.contains("\"users\""));
        assertTrue(sql.contains("ADD COLUMN"));
        assertTrue(sql.contains("\"role\""));
        assertTrue(sql.contains("VARCHAR(50)"));
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        AddColumnChange change = createSimpleAddColumnChange();
        change.setSchemaName("public");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"public\".\"users\""));
    }

    @Test
    void testGenerateSql_WithDefaultValue() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("status");
        column.setType("VARCHAR(20)");
        column.setDefaultValue("active");
        column.setConstraints(new ConstraintsConfig(false, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("DEFAULT 'active'"));
        assertTrue(sql.contains("NOT NULL"));
    }

    @Test
    void testGenerateSql_WithNotNull() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("email");
        column.setType("VARCHAR(255)");
        column.setConstraints(new ConstraintsConfig(false, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("NOT NULL"));
    }

    @Test
    void testGenerateSql_WithUnique() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("username");
        column.setType("VARCHAR(100)");
        column.setConstraints(new ConstraintsConfig(false, null, true, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("UNIQUE"));
    }

    @Test
    void testGenerateSql_MultipleColumns() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column1 = new ColumnConfig();
        column1.setName("role");
        column1.setType("VARCHAR(50)");
        column1.setConstraints(new ConstraintsConfig(false, null, null, null, null, null, null, null));

        ColumnConfig column2 = new ColumnConfig();
        column2.setName("status");
        column2.setType("VARCHAR(20)");
        column2.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(column1, column2));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"role\" VARCHAR(50)"));
        assertTrue(sql.contains("\"status\" VARCHAR(20)"));
        // Should have separate statements
        assertTrue(sql.contains(";"));
    }

    @Test
    void testExecute_MultipleColumns_ExecutesMultipleStatements() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column1 = new ColumnConfig();
        column1.setName("role");
        column1.setType("VARCHAR(50)");
        column1.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        ColumnConfig column2 = new ColumnConfig();
        column2.setName("status");
        column2.setType("VARCHAR(20)");
        column2.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(column1, column2));

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(2)).execute(anyString());
    }

    @Test
    void testGenerateSql_WithCheckConstraint() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("products");

        ColumnConfig column = new ColumnConfig();
        column.setName("price");
        column.setType("DECIMAL(10,2)");
        column.setConstraints(new ConstraintsConfig(false, null, null, null, null, "price > 0", null, null));

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("CHECK (price > 0)"));
    }

    @Test
    void testGenerateSql_CompleteExample() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setSchemaName("public");
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("role");
        column.setType("VARCHAR(50)");
        column.setDefaultValue("USER");
        column.setConstraints(new ConstraintsConfig(false, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("ALTER TABLE \"public\".\"users\""));
        assertTrue(sql.contains("ADD COLUMN"));
        assertTrue(sql.contains("\"role\" VARCHAR(50)"));
        assertTrue(sql.contains("DEFAULT 'USER'"));
        assertTrue(sql.contains("NOT NULL"));
    }

    private AddColumnChange createSimpleAddColumnChange() {
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("role");
        column.setType("VARCHAR(50)");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));
        return change;
    }
}
