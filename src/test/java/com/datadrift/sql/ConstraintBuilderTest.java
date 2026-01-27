package com.datadrift.sql;

import com.datadrift.model.change.CreateTableChange.ColumnConfig;
import com.datadrift.model.change.CreateTableChange.ConstraintsConfig;
import com.datadrift.model.change.CreateTableChange.ForeignKeyAction;
import com.datadrift.model.change.CreateTableChange.UniqueConstraint;
import com.datadrift.model.change.CreateTableChange.TableCheckConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintBuilderTest {

    private ConstraintBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ConstraintBuilder();
    }

    @Test
    void testBuildPrimaryKey_SingleColumn() {
        // Given
        List<String> columns = List.of("id");

        // When
        String result = builder.buildPrimaryKey(columns);

        // Then
        assertEquals("PRIMARY KEY (\"id\")", result);
    }

    @Test
    void testBuildPrimaryKey_CompositeKey() {
        // Given
        List<String> columns = List.of("user_id", "role_id");

        // When
        String result = builder.buildPrimaryKey(columns);

        // Then
        assertEquals("PRIMARY KEY (\"user_id\", \"role_id\")", result);
    }

    @Test
    void testBuildPrimaryKey_MultipleColumns() {
        // Given
        List<String> columns = List.of("country", "state", "city");

        // When
        String result = builder.buildPrimaryKey(columns);

        // Then
        assertEquals("PRIMARY KEY (\"country\", \"state\", \"city\")", result);
    }

    @Test
    void testBuildPrimaryKey_ThrowsException_WhenColumnsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildPrimaryKey(null));
    }

    @Test
    void testBuildPrimaryKey_ThrowsException_WhenColumnsEmpty() {
        // Given
        List<String> columns = List.of();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildPrimaryKey(columns));
    }

    @Test
    void testBuildPrimaryKey_EscapesSpecialCharacters() {
        // Given
        List<String> columns = List.of("user-id", "order-number");

        // When
        String result = builder.buildPrimaryKey(columns);

        // Then
        assertTrue(result.contains("\"user-id\""));
        assertTrue(result.contains("\"order-number\""));
    }

    @Test
    void testBuildForeignKey_Simple() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "users(id)",
                null,
                null,
                null
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("FOREIGN KEY (\"user_id\")"));
        assertTrue(result.contains("REFERENCES users(id)"));
    }

    @Test
    void testBuildForeignKey_WithConstraintName() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                "fk_orders_users",
                "users(id)",
                null,
                null,
                null
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("CONSTRAINT \"fk_orders_users\""));
        assertTrue(result.contains("FOREIGN KEY (\"user_id\")"));
        assertTrue(result.contains("REFERENCES users(id)"));
    }

    @Test
    void testBuildForeignKey_WithOnDeleteCascade() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "users(id)",
                null,
                ForeignKeyAction.CASCADE,
                null
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("ON DELETE CASCADE"));
    }

    @Test
    void testBuildForeignKey_WithOnDeleteSetNull() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "users(id)",
                null,
                ForeignKeyAction.SET_NULL,
                null
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("ON DELETE SET NULL"));
    }

    @Test
    void testBuildForeignKey_WithOnDeleteRestrict() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "users(id)",
                null,
                ForeignKeyAction.RESTRICT,
                null
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("ON DELETE RESTRICT"));
    }

    @Test
    void testBuildForeignKey_WithOnDeleteNoAction() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "users(id)",
                null,
                ForeignKeyAction.NO_ACTION,
                null
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("ON DELETE NO ACTION"));
    }

    @Test
    void testBuildForeignKey_WithOnDeleteSetDefault() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "users(id)",
                null,
                ForeignKeyAction.SET_DEFAULT,
                null
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("ON DELETE SET DEFAULT"));
    }

    @Test
    void testBuildForeignKey_WithOnUpdateCascade() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "users(id)",
                null,
                null,
                ForeignKeyAction.CASCADE
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("ON UPDATE CASCADE"));
    }

    @Test
    void testBuildForeignKey_WithBothOnDeleteAndOnUpdate() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "users(id)",
                null,
                ForeignKeyAction.CASCADE,
                ForeignKeyAction.NO_ACTION
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("ON DELETE CASCADE"));
        assertTrue(result.contains("ON UPDATE NO ACTION"));
    }

    @Test
    void testBuildForeignKey_Complete() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                "fk_orders_users",
                "users(id)",
                null,
                ForeignKeyAction.CASCADE,
                ForeignKeyAction.RESTRICT
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("CONSTRAINT \"fk_orders_users\""));
        assertTrue(result.contains("FOREIGN KEY (\"user_id\")"));
        assertTrue(result.contains("REFERENCES users(id)"));
        assertTrue(result.contains("ON DELETE CASCADE"));
        assertTrue(result.contains("ON UPDATE RESTRICT"));
    }

    @Test
    void testBuildForeignKey_WithSchemaQualifiedReference() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "public.users(id)",
                null,
                null,
                null
        ));

        // When
        String result = builder.buildForeignKey(column);

        // Then
        assertTrue(result.contains("REFERENCES public.users(id)"));
    }

    @Test
    void testBuildForeignKey_ThrowsException_WhenConstraintsNull() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildForeignKey(column));
    }

    @Test
    void testBuildForeignKey_ThrowsException_WhenNoForeignKey() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                null, // No references
                null,
                null,
                null
        ));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildForeignKey(column));
    }

    @Test
    void testBuildForeignKey_ThrowsException_WhenReferencesEmpty() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "",
                null,
                null,
                null
        ));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildForeignKey(column));
    }

    @Test
    void testBuildForeignKey_ThrowsException_WhenInvalidReferencesFormat() {
        // Given
        ColumnConfig column = new ColumnConfig();
        column.setName("user_id");
        column.setConstraints(new ConstraintsConfig(
                null, null, null,
                null,
                "users", // Missing column
                null,
                null,
                null
        ));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildForeignKey(column));
    }

    @Test
    void testBuildUnique_SingleColumn() {
        // Given
        UniqueConstraint constraint = new UniqueConstraint();
        constraint.setColumns(List.of("email"));

        // When
        String result = builder.buildUnique(constraint);

        // Then
        assertEquals("UNIQUE (\"email\")", result);
    }

    @Test
    void testBuildUnique_MultipleColumns() {
        // Given
        UniqueConstraint constraint = new UniqueConstraint();
        constraint.setColumns(List.of("email", "username"));

        // When
        String result = builder.buildUnique(constraint);

        // Then
        assertEquals("UNIQUE (\"email\", \"username\")", result);
    }

    @Test
    void testBuildUnique_WithConstraintName() {
        // Given
        UniqueConstraint constraint = new UniqueConstraint();
        constraint.setConstraintName("uq_users_email");
        constraint.setColumns(List.of("email"));

        // When
        String result = builder.buildUnique(constraint);

        // Then
        assertTrue(result.contains("CONSTRAINT \"uq_users_email\""));
        assertTrue(result.contains("UNIQUE (\"email\")"));
    }

    @Test
    void testBuildUnique_ThrowsException_WhenColumnsNull() {
        // Given
        UniqueConstraint constraint = new UniqueConstraint();
        constraint.setColumns(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildUnique(constraint));
    }

    @Test
    void testBuildUnique_ThrowsException_WhenColumnsEmpty() {
        // Given
        UniqueConstraint constraint = new UniqueConstraint();
        constraint.setColumns(List.of());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildUnique(constraint));
    }

    @Test
    void testBuildCheck_Simple() {
        // Given
        TableCheckConstraint constraint = new TableCheckConstraint();
        constraint.setCheckExpression("age >= 18");

        // When
        String result = builder.buildCheck(constraint);

        // Then
        assertEquals("CHECK (age >= 18)", result);
    }

    @Test
    void testBuildCheck_WithConstraintName() {
        // Given
        TableCheckConstraint constraint = new TableCheckConstraint();
        constraint.setConstraintName("chk_adult_age");
        constraint.setCheckExpression("age >= 18");

        // When
        String result = builder.buildCheck(constraint);

        // Then
        assertTrue(result.contains("CONSTRAINT \"chk_adult_age\""));
        assertTrue(result.contains("CHECK (age >= 18)"));
    }

    @Test
    void testBuildCheck_ComplexExpression() {
        // Given
        TableCheckConstraint constraint = new TableCheckConstraint();
        constraint.setCheckExpression("price > 0 AND price < 1000");

        // When
        String result = builder.buildCheck(constraint);

        // Then
        assertTrue(result.contains("CHECK (price > 0 AND price < 1000)"));
    }

    @Test
    void testBuildCheck_ThrowsException_WhenExpressionNull() {
        // Given
        TableCheckConstraint constraint = new TableCheckConstraint();
        constraint.setCheckExpression(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildCheck(constraint));
    }

    @Test
    void testBuildCheck_ThrowsException_WhenExpressionBlank() {
        // Given
        TableCheckConstraint constraint = new TableCheckConstraint();
        constraint.setCheckExpression("   ");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildCheck(constraint));
    }

    @Test
    void testBuildCheck_ThrowsException_WhenExpressionContainsSemicolon() {
        // Given
        TableCheckConstraint constraint = new TableCheckConstraint();
        constraint.setCheckExpression("age > 0; DROP TABLE users");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildCheck(constraint));
    }

    @Test
    void testBuildCheck_ThrowsException_WhenExpressionContainsSqlComment() {
        // Given
        TableCheckConstraint constraint = new TableCheckConstraint();
        constraint.setCheckExpression("age > 0 -- malicious comment");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> builder.buildCheck(constraint));
    }

    @Test
    void testMultipleConstraints_AllTypes() {
        // Test that all constraint types work together correctly

        // Primary Key
        String pk = builder.buildPrimaryKey(List.of("id"));
        assertTrue(pk.contains("PRIMARY KEY"));

        // Foreign Key
        ColumnConfig fkColumn = new ColumnConfig();
        fkColumn.setName("user_id");
        fkColumn.setConstraints(new ConstraintsConfig(
                null, null, null,
                null, "users(id)", null,
                ForeignKeyAction.CASCADE, null
        ));
        String fk = builder.buildForeignKey(fkColumn);
        assertTrue(fk.contains("FOREIGN KEY"));

        // Unique
        UniqueConstraint uniqueConstraint = new UniqueConstraint();
        uniqueConstraint.setColumns(List.of("email"));
        String unique = builder.buildUnique(uniqueConstraint);
        assertTrue(unique.contains("UNIQUE"));

        // Check
        TableCheckConstraint checkConstraint = new TableCheckConstraint();
        checkConstraint.setCheckExpression("age >= 0");
        String check = builder.buildCheck(checkConstraint);
        assertTrue(check.contains("CHECK"));
    }

    @Test
    void testConstraintNames_AllowSpecialCharacters() {
        // Foreign Key
        ColumnConfig fkColumn = new ColumnConfig();
        fkColumn.setName("user_id");
        fkColumn.setConstraints(new ConstraintsConfig(
                null, null, null,
                "fk_orders-users",
                "users(id)", null, null, null
        ));
        String fk = builder.buildForeignKey(fkColumn);
        assertTrue(fk.contains("\"fk_orders-users\""));

        // Unique
        UniqueConstraint uniqueConstraint = new UniqueConstraint();
        uniqueConstraint.setConstraintName("uq_user-email");
        uniqueConstraint.setColumns(List.of("email"));
        String unique = builder.buildUnique(uniqueConstraint);
        assertTrue(unique.contains("\"uq_user-email\""));

        // Check
        TableCheckConstraint checkConstraint = new TableCheckConstraint();
        checkConstraint.setConstraintName("chk_valid-age");
        checkConstraint.setCheckExpression("age >= 0");
        String check = builder.buildCheck(checkConstraint);
        assertTrue(check.contains("\"chk_valid-age\""));
    }
}
