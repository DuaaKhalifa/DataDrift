package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DropTableChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        DropTableChange change = new DropTableChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("dropTable", result);
    }

    @Test
    void testValidate_Success() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setSchemaName("public");
        change.setTableName("users");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithCascade() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");
        change.setCascade(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithIfExists() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setTableName("users");
        change.setIfExists(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithAllOptions() {
        // Given
        DropTableChange change = new DropTableChange();
        change.setSchemaName("public");
        change.setTableName("users");
        change.setCascade(true);
        change.setIfExists(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_ThrowsException_WhenTableNameNull() {
        // Given
        DropTableChange change = new DropTableChange();
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
        DropTableChange change = new DropTableChange();
        change.setTableName("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("tableName is required"));
    }
}
