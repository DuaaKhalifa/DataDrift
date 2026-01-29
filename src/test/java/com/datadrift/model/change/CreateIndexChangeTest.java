package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateIndexChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        CreateIndexChange change = new CreateIndexChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("createIndex", result);
    }

    @Test
    void testValidate_Success_SingleColumn() {
        // Given
        CreateIndexChange change = new CreateIndexChange();
        change.setTableName("users");
        change.setColumns(List.of("email"));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_MultipleColumns() {
        // Given
        CreateIndexChange change = new CreateIndexChange();
        change.setTableName("users");
        change.setColumns(List.of("name", "email"));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        CreateIndexChange change = new CreateIndexChange();
        change.setSchemaName("public");
        change.setTableName("users");
        change.setColumns(List.of("email"));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithIndexName() {
        // Given
        CreateIndexChange change = new CreateIndexChange();
        change.setIndexName("idx_users_email");
        change.setTableName("users");
        change.setColumns(List.of("email"));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_UniqueIndex() {
        // Given
        CreateIndexChange change = new CreateIndexChange();
        change.setTableName("users");
        change.setColumns(List.of("username"));
        change.setUnique(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_ThrowsException_WhenTableNameNull() {
        // Given
        CreateIndexChange change = new CreateIndexChange();
        change.setTableName(null);
        change.setColumns(List.of("email"));

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
        CreateIndexChange change = new CreateIndexChange();
        change.setTableName("   ");
        change.setColumns(List.of("email"));

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
        CreateIndexChange change = new CreateIndexChange();
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
        CreateIndexChange change = new CreateIndexChange();
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
        CreateIndexChange change = new CreateIndexChange();
        change.setTableName("users");
        change.setColumns(List.of("email", "   "));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column name cannot be null or blank"));
    }
}
