package com.datadrift.util;

import java.util.regex.Pattern;

/**
 * Utility class for secure SQL escaping and validation.
 * Prevents SQL injection by properly escaping identifiers and literals.
 */
public final class SqlEscapeUtil {

    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    public static String escapeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier cannot be null or blank");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public static String escapeStringLiteral(String literal) {
        if (literal == null) {
            throw new IllegalArgumentException("String literal cannot be null");
        }
        return "'" + literal.replace("'", "''") + "'";
    }

    public static void validateExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression cannot be null or blank");
        }
        // Basic validation - allow common SQL expression characters
        // For production, consider more strict validation or use parameterized queries
        if (expression.contains(";") || expression.toLowerCase().contains("--") || expression.contains("/*")) {
            throw new IllegalArgumentException("Invalid SQL expression: " + expression);
        }
    }

    public static void validateIdentifier(String identifier, boolean allowSpecialChars) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier cannot be null or blank");
        }
        if (!allowSpecialChars && !VALID_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "Identifier '" + identifier + "' contains invalid characters. " +
                    "Use only letters, numbers, and underscores, starting with a letter or underscore."
            );
        }
    }

    public static String qualifiedName(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null && !parts[i].isBlank()) {
                if (sb.length() > 0) {
                    sb.append(".");
                }
                sb.append(escapeIdentifier(parts[i]));
            }
        }
        return sb.toString();
    }
}
