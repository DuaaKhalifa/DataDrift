package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DropForeignKeyChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("dropForeignKey", result);
    }

    @Test
    void testValidate_Success() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();
        change.setBaseSchemaName("public");
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithCascade() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setCascade(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithIfExists() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setIfExists(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithAllOptions() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();
        change.setBaseSchemaName("public");
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setCascade(true);
        change.setIfExists(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_ThrowsException_WhenBaseTableNameNull() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();
        change.setBaseTableName(null);
        change.setConstraintName("fk_orders_users");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("baseTableName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenBaseTableNameBlank() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();
        change.setBaseTableName("   ");
        change.setConstraintName("fk_orders_users");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("baseTableName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenConstraintNameNull() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();
        change.setBaseTableName("orders");
        change.setConstraintName(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("constraintName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenConstraintNameBlank() {
        // Given
        DropForeignKeyChange change = new DropForeignKeyChange();
        change.setBaseTableName("orders");
        change.setConstraintName("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("constraintName is required"));
    }
}
