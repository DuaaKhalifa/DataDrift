package com.datadrift.executor.change;

import com.datadrift.model.change.ColumnValue;
import com.datadrift.model.change.UpdateChange;
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
class UpdateExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private UpdateExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new UpdateExecutor(jdbcTemplate);
    }

    @Test
    void testExecute_SimpleUpdate() {
        // Given
        UpdateChange change = createSimpleUpdate();

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testGenerateSql_StringValue() {
        // Given
        UpdateChange change = createSimpleUpdate();

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("SET \"username\" = 'new_name'"));
        assertTrue(sql.contains("WHERE id = 1"));
    }

    @Test
    void testGenerateSql_NumericValue() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("age");
        column.setValue("30");
        column.setValueType("NUMERIC");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("SET \"age\" = 30"));
        assertFalse(sql.contains("'30'"));  // Should not have quotes
    }

    @Test
    void testGenerateSql_BooleanValue() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("is_active");
        column.setValue("false");
        column.setValueType("BOOLEAN");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("SET \"is_active\" = false"));
    }

    @Test
    void testGenerateSql_NullValue() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("middle_name");
        column.setValue(null);
        column.setValueType("NULL");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("SET \"middle_name\" = NULL"));
    }

    @Test
    void testGenerateSql_TimestampValue() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("updated_at");
        column.setValue("2024-01-01 12:00:00");
        column.setValueType("TIMESTAMP");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("SET \"updated_at\" = '2024-01-01 12:00:00'"));
    }

    @Test
    void testGenerateSql_DateValue() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("birth_date");
        column.setValue("1990-01-01");
        column.setValueType("DATE");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("SET \"birth_date\" = '1990-01-01'"));
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        UpdateChange change = createSimpleUpdate();
        change.setSchemaName("public");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("UPDATE \"public\".\"users\""));
    }

    @Test
    void testGenerateSql_MultipleColumns() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue col1 = new ColumnValue();
        col1.setName("username");
        col1.setValue("new_name");
        col1.setValueType("STRING");

        ColumnValue col2 = new ColumnValue();
        col2.setName("age");
        col2.setValue("30");
        col2.setValueType("NUMERIC");

        ColumnValue col3 = new ColumnValue();
        col3.setName("is_active");
        col3.setValue("true");
        col3.setValueType("BOOLEAN");

        change.setColumns(List.of(col1, col2, col3));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("SET \"username\" = 'new_name', \"age\" = 30, \"is_active\" = true"));
        assertTrue(sql.contains("WHERE id = 1"));
    }

    @Test
    void testGenerateSql_ComplexWhereClause() {
        // Given
        UpdateChange change = createSimpleUpdate();
        change.setWhere("id > 10 AND status = 'active'");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("WHERE id > 10 AND status = 'active'"));
    }

    @Test
    void testGenerateSql_CompleteExample() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setSchemaName("public");
        change.setTableName("users");
        change.setWhere("email = 'old@example.com'");

        ColumnValue col1 = new ColumnValue();
        col1.setName("email");
        col1.setValue("new@example.com");
        col1.setValueType("STRING");

        ColumnValue col2 = new ColumnValue();
        col2.setName("is_active");
        col2.setValue("false");
        col2.setValueType("BOOLEAN");

        change.setColumns(List.of(col1, col2));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("UPDATE \"public\".\"users\""));
        assertTrue(sql.contains("SET \"email\" = 'new@example.com', \"is_active\" = false"));
        assertTrue(sql.contains("WHERE email = 'old@example.com'"));
    }

    @Test
    void testGenerateSql_EscapesStringValues() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("bio");
        column.setValue("I'm a developer");
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("'I''m a developer'"));  // Single quote should be escaped
    }

    @Test
    void testGenerateSql_ThrowsException_InvalidNumericValue() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("age");
        column.setValue("not_a_number");
        column.setValueType("NUMERIC");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.generateSql(change)
        );
        assertTrue(exception.getMessage().contains("Invalid NUMERIC value"));
    }

    @Test
    void testGenerateSql_ThrowsException_NullNumericValue() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("age");
        column.setValue(null);
        column.setValueType("NUMERIC");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.generateSql(change)
        );
        assertTrue(exception.getMessage().contains("NUMERIC value cannot be null or blank"));
    }

    @Test
    void testGenerateSql_ThrowsException_InvalidBooleanValue() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("is_active");
        column.setValue("maybe");
        column.setValueType("BOOLEAN");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.generateSql(change)
        );
        assertTrue(exception.getMessage().contains("Invalid BOOLEAN value"));
    }

    @Test
    void testGenerateSql_ThrowsException_WhenWhereCauseSqlInjection() {
        // Given
        UpdateChange change = createSimpleUpdate();
        change.setWhere("id = 1; DROP TABLE users;--");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.generateSql(change)
        );
        assertTrue(exception.getMessage().contains("Invalid SQL expression"));
    }

    @Test
    void testGenerateSql_StringValueCanBeNull() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("bio");
        column.setValue(null);
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("SET \"bio\" = NULL"));
    }

    private UpdateChange createSimpleUpdate() {
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("new_name");
        column.setValueType("STRING");

        change.setColumns(List.of(column));
        return change;
    }
}
