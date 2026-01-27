package com.datadrift.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlEscapeUtilTest {

    @Test
    void testEscapeIdentifier_Simple() {
        // When
        String result = SqlEscapeUtil.escapeIdentifier("users");

        // Then
        assertEquals("\"users\"", result);
    }

    @Test
    void testEscapeIdentifier_WithUnderscores() {
        // When
        String result = SqlEscapeUtil.escapeIdentifier("user_name");

        // Then
        assertEquals("\"user_name\"", result);
    }

    @Test
    void testEscapeIdentifier_WithNumbers() {
        // When
        String result = SqlEscapeUtil.escapeIdentifier("table123");

        // Then
        assertEquals("\"table123\"", result);
    }

    @Test
    void testEscapeIdentifier_WithDoubleQuotes() {
        // When
        String result = SqlEscapeUtil.escapeIdentifier("user\"name");

        // Then
        assertEquals("\"user\"\"name\"", result);
    }

    @Test
    void testEscapeIdentifier_WithSpecialCharacters() {
        // When
        String result = SqlEscapeUtil.escapeIdentifier("user-name");

        // Then
        assertEquals("\"user-name\"", result);
    }

    @Test
    void testEscapeIdentifier_WithSpaces() {
        // When
        String result = SqlEscapeUtil.escapeIdentifier("user name");

        // Then
        assertEquals("\"user name\"", result);
    }

    @Test
    void testEscapeIdentifier_ThrowsException_WhenNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.escapeIdentifier(null));
    }

    @Test
    void testEscapeIdentifier_ThrowsException_WhenBlank() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.escapeIdentifier("   "));
    }

    @Test
    void testEscapeIdentifier_ThrowsException_WhenEmpty() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.escapeIdentifier(""));
    }

    @Test
    void testEscapeStringLiteral_Simple() {
        // When
        String result = SqlEscapeUtil.escapeStringLiteral("hello");

        // Then
        assertEquals("'hello'", result);
    }

    @Test
    void testEscapeStringLiteral_WithSingleQuote() {
        // When
        String result = SqlEscapeUtil.escapeStringLiteral("it's");

        // Then
        assertEquals("'it''s'", result);
    }

    @Test
    void testEscapeStringLiteral_WithMultipleSingleQuotes() {
        // When
        String result = SqlEscapeUtil.escapeStringLiteral("O'Reilly's book");

        // Then
        assertEquals("'O''Reilly''s book'", result);
    }

    @Test
    void testEscapeStringLiteral_EmptyString() {
        // When
        String result = SqlEscapeUtil.escapeStringLiteral("");

        // Then
        assertEquals("''", result);
    }

    @Test
    void testEscapeStringLiteral_WithSpecialCharacters() {
        // When
        String result = SqlEscapeUtil.escapeStringLiteral("hello@world!#$%");

        // Then
        assertEquals("'hello@world!#$%'", result);
    }

    @Test
    void testEscapeStringLiteral_WithNewlines() {
        // When
        String result = SqlEscapeUtil.escapeStringLiteral("line1\nline2");

        // Then
        assertEquals("'line1\nline2'", result);
    }

    @Test
    void testEscapeStringLiteral_ThrowsException_WhenNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.escapeStringLiteral(null));
    }

    @Test
    void testValidateExpression_Simple() {
        // Should not throw exception
        assertDoesNotThrow(() -> SqlEscapeUtil.validateExpression("age > 18"));
    }

    @Test
    void testValidateExpression_WithNumbers() {
        // Should not throw exception
        assertDoesNotThrow(() -> SqlEscapeUtil.validateExpression("price * 0.15"));
    }

    @Test
    void testValidateExpression_WithParentheses() {
        // Should not throw exception
        assertDoesNotThrow(() -> SqlEscapeUtil.validateExpression("(price + tax) * quantity"));
    }

    @Test
    void testValidateExpression_WithComparison() {
        // Should not throw exception
        assertDoesNotThrow(() -> SqlEscapeUtil.validateExpression("age >= 0 AND age <= 100"));
    }

    @Test
    void testValidateExpression_WithFunctionCalls() {
        // Should not throw exception
        assertDoesNotThrow(() -> SqlEscapeUtil.validateExpression("UPPER(name)"));
    }

    @Test
    void testValidateExpression_ThrowsException_WhenNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateExpression(null));
    }

    @Test
    void testValidateExpression_ThrowsException_WhenBlank() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateExpression("   "));
    }

    @Test
    void testValidateExpression_ThrowsException_WhenContainsSemicolon() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateExpression("age > 0; DROP TABLE users"));
    }

    @Test
    void testValidateExpression_ThrowsException_WhenContainsSqlComment() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateExpression("age > 0 -- comment"));
    }

    @Test
    void testValidateExpression_ThrowsException_WhenContainsSqlCommentInMiddle() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateExpression("age > 0 --malicious\n AND age < 100"));
    }

    @Test
    void testValidateIdentifier_Simple_Strict() {
        // Should not throw exception
        assertDoesNotThrow(() -> SqlEscapeUtil.validateIdentifier("users", false));
    }

    @Test
    void testValidateIdentifier_WithUnderscores_Strict() {
        // Should not throw exception
        assertDoesNotThrow(() -> SqlEscapeUtil.validateIdentifier("user_name", false));
    }

    @Test
    void testValidateIdentifier_StartsWithUnderscore_Strict() {
        // Should not throw exception
        assertDoesNotThrow(() -> SqlEscapeUtil.validateIdentifier("_users", false));
    }

    @Test
    void testValidateIdentifier_WithNumbers_Strict() {
        // Should not throw exception
        assertDoesNotThrow(() -> SqlEscapeUtil.validateIdentifier("user123", false));
    }

    @Test
    void testValidateIdentifier_AllowsSpecialChars() {
        // Should not throw exception when allowSpecialChars is true
        assertDoesNotThrow(() -> SqlEscapeUtil.validateIdentifier("user-name", true));
    }

    @Test
    void testValidateIdentifier_ThrowsException_WhenNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateIdentifier(null, false));
    }

    @Test
    void testValidateIdentifier_ThrowsException_WhenBlank() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateIdentifier("   ", false));
    }

    @Test
    void testValidateIdentifier_ThrowsException_WhenStartsWithNumber_Strict() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateIdentifier("123users", false));
    }

    @Test
    void testValidateIdentifier_ThrowsException_WhenHasHyphen_Strict() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateIdentifier("user-name", false));
    }

    @Test
    void testValidateIdentifier_ThrowsException_WhenHasSpace_Strict() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateIdentifier("user name", false));
    }

    @Test
    void testValidateIdentifier_ThrowsException_WhenHasSpecialChars_Strict() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateIdentifier("user@name", false));
    }

    @Test
    void testQualifiedName_SinglePart() {
        // When
        String result = SqlEscapeUtil.qualifiedName("users");

        // Then
        assertEquals("\"users\"", result);
    }

    @Test
    void testQualifiedName_TwoParts() {
        // When
        String result = SqlEscapeUtil.qualifiedName("public", "users");

        // Then
        assertEquals("\"public\".\"users\"", result);
    }

    @Test
    void testQualifiedName_ThreeParts() {
        // When
        String result = SqlEscapeUtil.qualifiedName("database", "public", "users");

        // Then
        assertEquals("\"database\".\"public\".\"users\"", result);
    }

    @Test
    void testQualifiedName_WithNullPart() {
        // When
        String result = SqlEscapeUtil.qualifiedName(null, "users");

        // Then
        assertEquals("\"users\"", result);
    }

    @Test
    void testQualifiedName_WithBlankPart() {
        // When
        String result = SqlEscapeUtil.qualifiedName("", "users");

        // Then
        assertEquals("\"users\"", result);
    }

    @Test
    void testQualifiedName_MultipleNullParts() {
        // When
        String result = SqlEscapeUtil.qualifiedName(null, "public", null, "users");

        // Then
        assertEquals("\"public\".\"users\"", result);
    }

    @Test
    void testQualifiedName_WithSpecialCharacters() {
        // When
        String result = SqlEscapeUtil.qualifiedName("my-schema", "user-table");

        // Then
        assertEquals("\"my-schema\".\"user-table\"", result);
    }

    @Test
    void testQualifiedName_EscapesQuotes() {
        // When
        String result = SqlEscapeUtil.qualifiedName("public", "user\"table");

        // Then
        assertEquals("\"public\".\"user\"\"table\"", result);
    }

    @Test
    void testQualifiedName_EmptyArray() {
        // When
        String result = SqlEscapeUtil.qualifiedName();

        // Then
        assertEquals("", result);
    }

    @Test
    void testQualifiedName_AllNull() {
        // When
        String result = SqlEscapeUtil.qualifiedName(null, null);

        // Then
        assertEquals("", result);
    }

    @Test
    void testQualifiedName_AllBlank() {
        // When
        String result = SqlEscapeUtil.qualifiedName("", "   ");

        // Then
        assertEquals("", result);
    }

    @Test
    void testSqlInjectionPrevention_Identifiers() {
        // Attempt SQL injection through identifier
        String malicious = "users; DROP TABLE important";
        String escaped = SqlEscapeUtil.escapeIdentifier(malicious);

        // Should be safely escaped
        assertEquals("\"users; DROP TABLE important\"", escaped);
        // When used in SQL, the semicolon is inside quotes, so it won't execute
    }

    @Test
    void testSqlInjectionPrevention_StringLiterals() {
        // Attempt SQL injection through string literal
        String malicious = "'; DROP TABLE users; --";
        String escaped = SqlEscapeUtil.escapeStringLiteral(malicious);

        // Should be safely escaped
        assertEquals("'''; DROP TABLE users; --'", escaped);
    }

    @Test
    void testSqlInjectionPrevention_Expressions() {
        // Attempt SQL injection through expression
        String malicious = "age > 0; DROP TABLE users";

        // Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> SqlEscapeUtil.validateExpression(malicious));
    }

    @Test
    void testEdgeCase_UnicodeCharacters() {
        // Test with Unicode characters
        String unicode = "用户表";
        String escaped = SqlEscapeUtil.escapeIdentifier(unicode);

        assertEquals("\"用户表\"", escaped);
    }

    @Test
    void testEdgeCase_VeryLongIdentifier() {
        // Test with very long identifier
        String longName = "a".repeat(1000);
        String escaped = SqlEscapeUtil.escapeIdentifier(longName);

        assertTrue(escaped.startsWith("\""));
        assertTrue(escaped.endsWith("\""));
        assertEquals(1002, escaped.length()); // original + 2 quotes
    }

    @Test
    void testEdgeCase_MultipleConsecutiveQuotes() {
        // Test escaping multiple consecutive quotes
        String identifier = "user\"\"\"name";
        String escaped = SqlEscapeUtil.escapeIdentifier(identifier);

        assertEquals("\"user\"\"\"\"\"\"name\"", escaped);
    }

    @Test
    void testRealWorldScenario_CompleteTableCreation() {
        // Test a real-world scenario
        String schema = "public";
        String table = "user_accounts";
        String column = "email_address";
        String defaultValue = "no-reply@example.com";

        String qualifiedTable = SqlEscapeUtil.qualifiedName(schema, table);
        String escapedColumn = SqlEscapeUtil.escapeIdentifier(column);
        String escapedDefault = SqlEscapeUtil.escapeStringLiteral(defaultValue);

        assertEquals("\"public\".\"user_accounts\"", qualifiedTable);
        assertEquals("\"email_address\"", escapedColumn);
        assertEquals("'no-reply@example.com'", escapedDefault);
    }
}
