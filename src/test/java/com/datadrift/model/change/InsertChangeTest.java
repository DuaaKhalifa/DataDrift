package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InsertChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        InsertChange change = new InsertChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("insert", result);
    }

    @Test
    void testValidate_Success_StringValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_NumericValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("age");
        column.setValue("25");
        column.setValueType("NUMERIC");

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_BooleanValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("is_active");
        column.setValue("true");
        column.setValueType("BOOLEAN");

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_NullValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("middle_name");
        column.setValue(null);
        column.setValueType("NULL");

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_TimestampValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("created_at");
        column.setValue("2024-01-01 12:00:00");
        column.setValueType("TIMESTAMP");

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_DateValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("birth_date");
        column.setValue("1990-01-01");
        column.setValueType("DATE");

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_MultipleColumns() {
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

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        InsertChange change = new InsertChange();
        change.setSchemaName("public");
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_ThrowsException_WhenTableNameNull() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName(null);

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("tableName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenTableNameBlank() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("   ");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("tableName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenColumnsNull() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");
        change.setColumns(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("At least one column is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenColumnsEmpty() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");
        change.setColumns(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("At least one column is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenColumnNameNull() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName(null);
        column.setValue("john_doe");
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column name cannot be null or blank"));
    }

    @Test
    void testValidate_ThrowsException_WhenColumnNameBlank() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("   ");
        column.setValue("john_doe");
        column.setValueType("STRING");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column name cannot be null or blank"));
    }

    @Test
    void testValidate_ThrowsException_WhenValueTypeNull() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType(null);

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column valueType is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenValueTypeBlank() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType("   ");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column valueType is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenValueTypeInvalid() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType("INVALID_TYPE");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Invalid valueType"));
    }

    @Test
    void testValidate_ThrowsException_WhenNullTypeHasNonNullValue() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("middle_name");
        column.setValue("some_value");
        column.setValueType("NULL");

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("has valueType NULL but value is not null"));
    }

    @Test
    void testValidate_AllValueTypes() {
        // Test all valid value types
        String[] validTypes = {"STRING", "NUMERIC", "BOOLEAN", "NULL", "TIMESTAMP", "DATE"};

        for (String type : validTypes) {
            // Given
            InsertChange change = new InsertChange();
            change.setTableName("users");

            ColumnValue column = new ColumnValue();
            column.setName("test_column");
            column.setValueType(type);

            if ("NULL".equals(type)) {
                column.setValue(null);
            } else {
                column.setValue("test_value");
            }

            change.setColumns(List.of(column));

            // When & Then
            assertDoesNotThrow(() -> change.validate(), "Should accept valueType: " + type);
        }
    }

    @Test
    void testValidate_ValueTypeCaseInsensitive() {
        // Given
        InsertChange change = new InsertChange();
        change.setTableName("users");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("john_doe");
        column.setValueType("string");  // lowercase

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }
}
