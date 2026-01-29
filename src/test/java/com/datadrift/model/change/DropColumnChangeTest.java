package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DropColumnChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        DropColumnChange change = new DropColumnChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("dropColumn", result);
    }

    @Test
    void testValidate_Success_SingleColumn() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setTableName("users");
        change.setColumns(List.of("role"));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_MultipleColumns() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setTableName("users");
        change.setColumns(List.of("role", "status"));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setSchemaName("public");
        change.setTableName("users");
        change.setColumns(List.of("role"));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithCascade() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setTableName("users");
        change.setColumns(List.of("role"));
        change.setCascade(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithIfExists() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setTableName("users");
        change.setColumns(List.of("role"));
        change.setIfExists(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithAllOptions() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setSchemaName("public");
        change.setTableName("users");
        change.setColumns(List.of("role", "status"));
        change.setCascade(true);
        change.setIfExists(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_ThrowsException_WhenTableNameNull() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setTableName(null);
        change.setColumns(List.of("role"));

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
        DropColumnChange change = new DropColumnChange();
        change.setTableName("   ");
        change.setColumns(List.of("role"));

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
        DropColumnChange change = new DropColumnChange();
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
        DropColumnChange change = new DropColumnChange();
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
    void testValidate_ThrowsException_WhenColumnNameBlank() {
        // Given
        DropColumnChange change = new DropColumnChange();
        change.setTableName("users");
        List<String> columns = new ArrayList<>();
        columns.add("role");
        columns.add("   ");
        change.setColumns(columns);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column name cannot be null or blank"));
    }
}
