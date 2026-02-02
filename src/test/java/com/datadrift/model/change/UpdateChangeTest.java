package com.datadrift.model.change;

import com.datadrift.model.change.InsertChange.ColumnValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpdateChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        UpdateChange change = new UpdateChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("update", result);
    }

    @Test
    void testValidate_Success_StringValue() {
        // Given
        UpdateChange change = createSimpleUpdate();

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        UpdateChange change = createSimpleUpdate();
        change.setSchemaName("public");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_MultipleColumns() {
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

        change.setColumns(List.of(col1, col2));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_AllValueTypes() {
        // Test all valid value types
        String[] validTypes = {"STRING", "NUMERIC", "BOOLEAN", "NULL", "TIMESTAMP", "DATE"};

        for (String type : validTypes) {
            // Given
            UpdateChange change = new UpdateChange();
            change.setTableName("users");
            change.setWhere("id = 1");

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
    void testValidate_ThrowsException_WhenTableNameNull() {
        // Given
        UpdateChange change = createSimpleUpdate();
        change.setTableName(null);

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
        UpdateChange change = createSimpleUpdate();
        change.setTableName("   ");

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
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");
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
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");
        change.setColumns(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("At least one column is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenWhereNull() {
        // Given
        UpdateChange change = createSimpleUpdate();
        change.setWhere(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("WHERE clause is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenWhereBlank() {
        // Given
        UpdateChange change = createSimpleUpdate();
        change.setWhere("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("WHERE clause is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenColumnNameNull() {
        // Given
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName(null);
        column.setValue("value");
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
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("   ");
        column.setValue("value");
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
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("value");
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
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("value");
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
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        ColumnValue column = new ColumnValue();
        column.setName("username");
        column.setValue("value");
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
        UpdateChange change = new UpdateChange();
        change.setTableName("users");
        change.setWhere("id = 1");

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
