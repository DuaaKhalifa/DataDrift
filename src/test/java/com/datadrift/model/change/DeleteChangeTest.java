package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeleteChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        DeleteChange change = new DeleteChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("delete", result);
    }

    @Test
    void testValidate_Success() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("id = 1");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setSchemaName("public");
        change.setTableName("users");
        change.setWhere("id = 1");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_ComplexWhere() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("status = 'inactive' AND created_at < '2020-01-01'");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_ThrowsException_WhenTableNameNull() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName(null);
        change.setWhere("id = 1");

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
        DeleteChange change = new DeleteChange();
        change.setTableName("   ");
        change.setWhere("id = 1");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("tableName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenWhereNull() {
        // Given
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
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
        DeleteChange change = new DeleteChange();
        change.setTableName("users");
        change.setWhere("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("WHERE clause is required"));
    }
}
