package com.datadrift.executor.change;

import com.datadrift.model.change.InsertChange;
import com.datadrift.model.change.InsertChange.ColumnValue;
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
class InsertExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private InsertExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new InsertExecutor(jdbcTemplate);
    }

    @Test
    void testExecute_SimpleInsert() {
        // Given
        InsertChange change = createSimpleInsert();

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void testGenerateSql_StringValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("(\"username\")"));
        assertTrue(sql.contains("VALUES ('john_doe')"));
    }

    @Test
    void testGenerateSql_NumericValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("age");
        column.setValue("25");
        column.setValueType("NUMERIC");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES (25)"));
        assertFalse(sql.contains("'25'"));  // Should not have quotes
    }

    @Test
    void testGenerateSql_NumericDecimalValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("products");

        ColumnValue column = new ColumnValue();
        column.setName("price");
        column.setValue("19.99");
        column.setValueType("NUMERIC");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES (19.99)"));
    }

    @Test
    void testGenerateSql_BooleanTrueValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("is_active");
        column.setValue("true");
        column.setValueType("BOOLEAN");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES (true)"));
    }

    @Test
    void testGenerateSql_BooleanFalseValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("is_active");
        column.setValue("false");
        column.setValueType("BOOLEAN");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES (false)"));
    }

    @Test
    void testGenerateSql_BooleanOneValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("is_active");
        column.setValue("1");
        column.setValueType("BOOLEAN");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES (true)"));
    }

    @Test
    void testGenerateSql_BooleanZeroValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("is_active");
        column.setValue("0");
        column.setValueType("BOOLEAN");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES (false)"));
    }

    @Test
    void testGenerateSql_NullValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("middle_name");
        column.setValue(null);
        column.setValueType("NULL");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES (NULL)"));
    }

    @Test
    void testGenerateSql_TimestampValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("created_at");
        column.setValue("2024-01-01 12:00:00");
        column.setValueType("TIMESTAMP");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES ('2024-01-01 12:00:00')"));
    }

    @Test
    void testGenerateSql_DateValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("birth_date");
        column.setValue("1990-01-01");
        column.setValueType("DATE");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES ('1990-01-01')"));
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        InsertChange change = createSimpleInsert();
        change.setSchemaName("public");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("INSERT INTO \"public\".\"users\""));
    }

    @Test
    void testGenerateSql_MultipleColumns() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue col1 = new ColumnValue();
        col1.setName("username");
        col1.setValue("john_doe");
        col1.setValueType("STRING");

        ColumnValue col2 = new ColumnValue();
        col2.setName("age");
        col2.setValue("25");
        col2.setValueType("NUMERIC");

        ColumnValue col3 = new ColumnValue();
        col3.setName("is_active");
        col3.setValue("true");
        col3.setValueType("BOOLEAN");

        change.setColumns(List.of(col1, col2, col3));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("(\"username\", \"age\", \"is_active\")"));
        assertTrue(sql.contains("VALUES ('john_doe', 25, true)"));
    }

    @Test
    void testGenerateSql_CompleteExample() {
        // Given
        InsertChange change = new InsertChange();
        change.setSchemaName("public");
        change.setTableName("users");

        ColumnValue col1 = new ColumnValue();
        col1.setName("username");
        col1.setValue("john_doe");
        col1.setValueType("STRING");

        ColumnValue col2 = new ColumnValue();
        col2.setName("email");
        col2.setValue("john@example.com");
        col2.setValueType("STRING");

        ColumnValue col3 = new ColumnValue();
        col3.setName("age");
        col3.setValue("30");
        col3.setValueType("NUMERIC");

        ColumnValue col4 = new ColumnValue();
        col4.setName("is_active");
        col4.setValue("true");
        col4.setValueType("BOOLEAN");

        ColumnValue col5 = new ColumnValue();
        col5.setName("middle_name");
        col5.setValue(null);
        col5.setValueType("NULL");

        change.setColumns(List.of(col1, col2, col3, col4, col5));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("INSERT INTO \"public\".\"users\""));
        assertTrue(sql.contains("(\"username\", \"email\", \"age\", \"is_active\", \"middle_name\")"));
        assertTrue(sql.contains("VALUES ('john_doe', 'john@example.com', 30, true, NULL)"));
    }

    @Test
    void testGenerateSql_EscapesStringValues() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

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
        InsertChange change = new InsertChange();
        change.setTableName("users");

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
        InsertChange change = new InsertChange();
        change.setTableName("users");

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
        InsertChange change = new InsertChange();
        change.setTableName("users");

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
    void testGenerateSql_ThrowsException_NullBooleanValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("is_active");
        column.setValue(null);
        column.setValueType("BOOLEAN");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.generateSql(change)
        );
        assertTrue(exception.getMessage().contains("BOOLEAN value cannot be null or blank"));
    }

    @Test
    void testGenerateSql_StringValueCanBeNull() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("bio");
        column.setValue(null);
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("VALUES (NULL)"));
    }

    private InsertChange createSimpleInsert() {
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType("STRING");

        change.setColumns(List.of(column));
        return change;
    }
}
