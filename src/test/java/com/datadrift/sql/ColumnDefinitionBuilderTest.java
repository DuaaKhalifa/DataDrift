package com.datadrift.sql;

import com.datadrift.model.change.CreateTableChange.ColumnConfig;
import com.datadrift.model.change.CreateTableChange.ConstraintsConfig;
import com.datadrift.sql.dialect.SqlDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ColumnDefinitionBuilderTest {

    @Mock
    private SqlDialect dialect;

    private ColumnDefinitionBuilder builder;

    @BeforeEach
    void setUp() {
        lenient().when(dialect.getAutoIncrementSyntax()).thenReturn("GENERATED ALWAYS AS IDENTITY");
        lenient().when(dialect.getGeneratedColumnSyntax(anyString(), anyBoolean()))
                .thenAnswer(invocation -> {
                    String expr = invocation.getArgument(0);
                    return "GENERATED ALWAYS AS (" + expr + ") STORED";
                });

        builder = new ColumnDefinitionBuilder(dialect);
    }

    @Test
    void testBuild_SimpleColumn() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("id");
        column.setType("INTEGER");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertEquals("\"id\" INTEGER", result);
    }

    @Test
    void testBuild_ColumnWithNotNull() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("name");
        column.setType("VARCHAR(100)");
        column.setConstraints(new ConstraintsConfig(false, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("\"name\" VARCHAR(100)"));
        assertTrue(result.contains("NOT NULL"));
    }

    @Test
    void testBuild_ColumnWithNull() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("description");
        column.setType("TEXT");
        column.setConstraints(new ConstraintsConfig(true, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("\"description\" TEXT"));
        assertTrue(result.contains("NULL"));
        assertFalse(result.contains("NOT NULL"));
    }

    @Test
    void testBuild_ColumnWithPrimaryKey() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("id");
        column.setType("INTEGER");
        column.setConstraints(new ConstraintsConfig(false, true, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("PRIMARY KEY"));
    }

    @Test
    void testBuild_ColumnWithUnique() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("email");
        column.setType("VARCHAR(255)");
        column.setConstraints(new ConstraintsConfig(false, null, true, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("UNIQUE"));
    }

    @Test
    void testBuild_ColumnWithCheckConstraint() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("age");
        column.setType("INTEGER");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, "age >= 0", null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("CHECK (age >= 0)"));
    }

    @Test
    void testBuild_ColumnWithDefaultValue_String() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("status");
        column.setType("VARCHAR(20)");
        column.setDefaultValue("active");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("DEFAULT 'active'"));
    }

    @Test
    void testBuild_ColumnWithDefaultValue_Number() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("count");
        column.setType("INTEGER");
        column.setDefaultValue(0);
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("DEFAULT 0"));
    }

    @Test
    void testBuild_ColumnWithDefaultValue_Boolean() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("is_active");
        column.setType("BOOLEAN");
        column.setDefaultValue(true);
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("DEFAULT true"));
    }

    @Test
    void testBuild_ColumnWithDefaultValue_SqlFunction() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("created_at");
        column.setType("TIMESTAMP");
        column.setDefaultValue("CURRENT_TIMESTAMP");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("DEFAULT CURRENT_TIMESTAMP"));
        // Should not be quoted
        assertFalse(result.contains("'CURRENT_TIMESTAMP'"));
    }

    @Test
    void testBuild_ColumnWithAutoIncrement() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("id");
        column.setType("INTEGER");
        column.setAutoIncrement(true);
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("GENERATED ALWAYS AS IDENTITY"));
        verify(dialect).getAutoIncrementSyntax();
    }

    @Test
    void testBuild_ColumnWithComputedValue() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("total");
        column.setType("DECIMAL(10,2)");
        column.setDefaultValueComputed("price * quantity");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("GENERATED ALWAYS AS (price * quantity) STORED"));
        verify(dialect).getGeneratedColumnSyntax("price * quantity", true);
    }

    @Test
    void testBuild_ComputedValueTakesPrecedenceOverDefault() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("total");
        column.setType("DECIMAL(10,2)");
        column.setDefaultValue(0);
        column.setDefaultValueComputed("price * quantity");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("GENERATED ALWAYS AS"));
        assertFalse(result.contains("DEFAULT 0"));
    }

    @Test
    void testBuild_ComplexColumn() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("email");
        column.setType("VARCHAR(255)");
        column.setConstraints(new ConstraintsConfig(false, null, true, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("\"email\" VARCHAR(255)"));
        assertTrue(result.contains("NOT NULL"));
        assertTrue(result.contains("UNIQUE"));
    }

    @Test
    void testBuild_AllConstraints() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("id");
        column.setType("INTEGER");
        column.setDefaultValue(1);
        column.setConstraints(new ConstraintsConfig(false, true, true, null, null, "id > 0", null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("\"id\" INTEGER"));
        assertTrue(result.contains("DEFAULT 1"));
        assertTrue(result.contains("NOT NULL"));
        assertTrue(result.contains("PRIMARY KEY"));
        assertTrue(result.contains("UNIQUE"));
        assertTrue(result.contains("CHECK (id > 0)"));
    }

    @Test
    void testBuild_ThrowsException_WhenNameIsNull() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName(null);
        column.setType("INTEGER");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.build(column));
    }

    @Test
    void testBuild_ThrowsException_WhenNameIsBlank() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("   ");
        column.setType("INTEGER");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.build(column));
    }

    @Test
    void testBuild_ThrowsException_WhenTypeIsNull() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("id");
        column.setType(null);
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.build(column));
    }

    @Test
    void testBuild_ThrowsException_WhenTypeIsBlank() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("id");
        column.setType("   ");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.build(column));
    }

    @Test
    void testBuild_NullSqlFunction() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("deleted_at");
        column.setType("TIMESTAMP");
        column.setDefaultValue("NULL");
        column.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result = builder.build(column);

        // Then
        assertTrue(result.contains("DEFAULT NULL"));
        assertFalse(result.contains("'NULL'"));
    }

    @Test
    void testBuild_BooleanSqlKeywords() {
        // Given
        ColumnConfig column1 = new ColumnConfig();
        column1.setName("flag1");
        column1.setType("BOOLEAN");
        column1.setDefaultValue("TRUE");
        column1.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        ColumnConfig column2 = new ColumnConfig();
        column2.setName("flag2");
        column2.setType("BOOLEAN");
        column2.setDefaultValue("FALSE");
        column2.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        // When
        String result1 = builder.build(column1);
        String result2 = builder.build(column2);

        // Then
        assertTrue(result1.contains("DEFAULT TRUE"));
        assertFalse(result1.contains("'TRUE'"));
        assertTrue(result2.contains("DEFAULT FALSE"));
        assertFalse(result2.contains("'FALSE'"));
    }
}
