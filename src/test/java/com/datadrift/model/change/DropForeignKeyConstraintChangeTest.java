package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DropForeignKeyConstraintChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("dropForeignKeyConstraint", result);
    }

    @Test
    void testValidate_Success() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseSchemaName("public");
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithCascade() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setCascade(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithIfExists() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
        change.setBaseTableName("orders");
        change.setConstraintName("fk_orders_users");
        change.setIfExists(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithAllOptions() {
        // Given
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
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
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
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
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
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
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
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
        DropForeignKeyConstraintChange change = new DropForeignKeyConstraintChange();
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
