package com.datadrift.model.change;

import com.datadrift.model.change.CreateTableChange.ColumnConfig;
import com.datadrift.model.change.CreateTableChange.ConstraintsConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddColumnChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        AddColumnChange change = new AddColumnChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("addColumn", result);
    }

    @Test
    void testValidate_Success_SingleColumn() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("role");
        column.setType("VARCHAR(50)");
        column.setConstraints(new ConstraintsConfig(false, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_MultipleColumns() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column1 = new ColumnConfig();
        column1.setName("role");
        column1.setType("VARCHAR(50)");
        column1.setConstraints(new ConstraintsConfig(false, null, null, null, null, null, null, null));

        ColumnConfig column2 = new ColumnConfig();
        column2.setName("status");
        column2.setType("VARCHAR(20)");
        column2.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(column1, column2));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSchema() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setSchemaName("public");
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("role");
        column.setType("VARCHAR(50)");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_ThrowsException_WhenTableNameNull() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName(null);

        ColumnConfig column = new ColumnConfig();
        column.setName("role");
        column.setType("VARCHAR(50)");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

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
        AddColumnChange change = new AddColumnChange();
        change.setTableName("   ");

        ColumnConfig column = new ColumnConfig();
        column.setName("role");
        column.setType("VARCHAR(50)");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

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
        AddColumnChange change = new AddColumnChange();
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
        AddColumnChange change = new AddColumnChange();
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
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName(null);
        column.setType("VARCHAR(50)");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column name is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenColumnNameBlank() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("   ");
        column.setType("VARCHAR(50)");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column name is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenColumnTypeNull() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("role");
        column.setType(null);
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column type is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenColumnTypeBlank() {
        // Given
        AddColumnChange change = new AddColumnChange();
        change.setTableName("users");

        ColumnConfig column = new ColumnConfig();
        column.setName("role");
        column.setType("   ");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(column));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("Column type is required"));
    }

    @Test
    void testSettersAndGetters() {
        // Given
        AddColumnChange change = new AddColumnChange();
        ColumnConfig column = new ColumnConfig();
        column.setName("role");
        column.setType("VARCHAR(50)");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        change.setTableName("users");
        change.setSchemaName("public");
        change.setColumns(List.of(column));

        // Then
        assertEquals("users", change.getTableName());
        assertEquals("public", change.getSchemaName());
        assertEquals(1, change.getColumns().size());
        assertEquals("role", change.getColumns().get(0).getName());
    }
}
