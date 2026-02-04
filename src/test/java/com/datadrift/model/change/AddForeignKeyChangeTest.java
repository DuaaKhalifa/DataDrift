package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddForeignKeyChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        AddForeignKeyChange change = new AddForeignKeyChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("addForeignKey", result);
    }

    @Test
    void testValidate_Success_MinimalConfiguration() {
        // Given
        AddForeignKeyChange change = createSimpleChange();

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setBaseSchemaName("public");
        change.setReferencedSchemaName("public");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithConstraintName() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setConstraintName("fk_orders_users");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithOnDelete() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setOnDelete("CASCADE");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithOnUpdate() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setOnUpdate("RESTRICT");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithDeferrable() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setDeferrable(true);
        change.setInitiallyDeferred(true);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithAllOptions() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setBaseSchemaName("public");
        change.setReferencedSchemaName("public");
        change.setConstraintName("fk_orders_users");
        change.setOnDelete("CASCADE");
        change.setOnUpdate("RESTRICT");
        change.setDeferrable(true);
        change.setInitiallyDeferred(false);

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_MultipleColumns() {
        // Given
        AddForeignKeyChange change = new AddForeignKeyChange();
        change.setBaseTableName("order_items");
        change.setBaseColumnNames(List.of("order_id", "product_id"));
        change.setReferencedTableName("orders");
        change.setReferencedColumnNames(List.of("id", "product_id"));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_AllForeignKeyActions() {
        // Given
        AddForeignKeyChange change = createSimpleChange();

        // Test all valid actions
        String[] validActions = {"CASCADE", "RESTRICT", "SET NULL", "SET DEFAULT", "NO ACTION"};
        for (String action : validActions) {
            change.setOnDelete(action);
            assertDoesNotThrow(() -> change.validate(), "Should accept: " + action);

            change.setOnUpdate(action);
            assertDoesNotThrow(() -> change.validate(), "Should accept: " + action);
        }
    }

    @Test
    void testValidate_ThrowsException_WhenBaseTableNameNull() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setBaseTableName(null);

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
        AddForeignKeyChange change = createSimpleChange();
        change.setBaseTableName("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("baseTableName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenBaseColumnsNull() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setBaseColumnNames(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("At least one base column is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenBaseColumnsEmpty() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setBaseColumnNames(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("At least one base column is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenReferencedTableNameNull() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setReferencedTableName(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("referencedTableName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenReferencedTableNameBlank() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setReferencedTableName("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("referencedTableName is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenReferencedColumnsNull() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setReferencedColumnNames(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("At least one referenced column is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenReferencedColumnsEmpty() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setReferencedColumnNames(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("At least one referenced column is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenColumnCountMismatch() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setBaseColumnNames(List.of("user_id", "product_id"));
        change.setReferencedColumnNames(List.of("id")); // Mismatch!

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Number of base columns must match number of referenced columns"));
    }

    @Test
    void testValidate_ThrowsException_WhenBaseColumnNameBlank() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        List<String> columns = new ArrayList<>();
        columns.add("user_id");
        columns.add("   ");
        change.setBaseColumnNames(columns);
        change.setReferencedColumnNames(List.of("id", "ref_id"));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Base column name cannot be null or blank"));
    }

    @Test
    void testValidate_ThrowsException_WhenReferencedColumnNameBlank() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setBaseColumnNames(List.of("user_id", "product_id"));
        List<String> refColumns = new ArrayList<>();
        refColumns.add("id");
        refColumns.add("   ");
        change.setReferencedColumnNames(refColumns);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Referenced column name cannot be null or blank"));
    }

    @Test
    void testValidate_ThrowsException_WhenInvalidOnDeleteAction() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setOnDelete("INVALID_ACTION");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Invalid onDelete action"));
    }

    @Test
    void testValidate_ThrowsException_WhenInvalidOnUpdateAction() {
        // Given
        AddForeignKeyChange change = createSimpleChange();
        change.setOnUpdate("INVALID_ACTION");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Invalid onUpdate action"));
    }

    private AddForeignKeyChange createSimpleChange() {
        AddForeignKeyChange change = new AddForeignKeyChange();
        change.setBaseTableName("orders");
        change.setBaseColumnNames(List.of("user_id"));
        change.setReferencedTableName("users");
        change.setReferencedColumnNames(List.of("id"));
        return change;
    }
}
