package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DropIndexChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        DropIndexChange change = new DropIndexChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("dropIndex", result);
    }

    @Test
    void testValidate_Success() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setSchemaName("public");
        change.setIndexName("idx_users_email");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithCascade() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");
        change.setCascade(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithIfExists() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("idx_users_email");
        change.setIfExists(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithAllOptions() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setSchemaName("public");
        change.setIndexName("idx_users_email");
        change.setCascade(true);
        change.setIfExists(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_ThrowsException_WhenIndexNameNull() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("indexName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenIndexNameBlank() {
        // Given
        DropIndexChange change = new DropIndexChange();
        change.setIndexName("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("indexName is required"));
    }
}
