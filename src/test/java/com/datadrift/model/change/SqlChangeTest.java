package com.datadrift.model.change;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlChangeTest {

    @Test
    void testGetChangeType() {
        // Given
        SqlChange change = new SqlChange();

        // When
        String result = change.getChangeType();

        // Then
        assertEquals("sql", result);
    }

    @Test
    void testValidate_Success_SimpleSql() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("SELECT * FROM users");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_ComplexSql() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("CREATE OR REPLACE FUNCTION test() RETURNS void AS $$ BEGIN END; $$ LANGUAGE plpgsql");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_MultiLineSql() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("CREATE TABLE test (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    name VARCHAR(100)\n" +
                ")");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithSemicolons() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("INSERT INTO users (name) VALUES ('John'); INSERT INTO users (name) VALUES ('Jane');");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_Success_WithComments() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("-- This is a comment\nSELECT * FROM users /* inline comment */");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }

    @Test
    void testValidate_ThrowsException_WhenSqlNull() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("sql is required"));
    }

    @Test
    void testValidate_ThrowsException_WhenSqlBlank() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> change.validate()
        );
        assertTrue(exception.getMessage().contains("sql is required"));
    }

    @Test
    void testValidate_Success_WithAllOptions() {
        // Given
        SqlChange change = new SqlChange();
        change.setSql("INSERT INTO users (name) VALUES ('John'); INSERT INTO users (name) VALUES ('Jane');");
        change.setDbms("postgresql");
        change.setStripComments(false);
        change.setSplitStatements(true);
        change.setEndDelimiter(";");

        // When & Then
        assertDoesNotThrow(() -> change.validate());
    }
}
