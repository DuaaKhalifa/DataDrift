package com.datadrift.sql;

import com.datadrift.sql.dialect.SqlDialect;
import com.datadrift.util.SqlEscapeUtil;

import java.util.ArrayList;
import java.util.List;

public class CreateTableSqlBuilder {

    private final SqlDialect dialect;
    private final StringBuilder mainStatement;
    private final List<String> definitions;
    private final List<String> postStatements;

    private String schemaName;
    private String tableName;
    private boolean ifNotExists;

    public CreateTableSqlBuilder(SqlDialect dialect) {
        this.dialect = dialect;
        this.mainStatement = new StringBuilder();
        this.definitions = new ArrayList<>();
        this.postStatements = new ArrayList<>();
    }

    public CreateTableSqlBuilder table(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public CreateTableSqlBuilder schema(String schemaName) {
        this.schemaName = schemaName;
        return this;
    }

    public CreateTableSqlBuilder ifNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
        return this;
    }

    public CreateTableSqlBuilder addDefinition(String definition) {
        this.definitions.add(definition);
        return this;
    }

    public CreateTableSqlBuilder tableComment(String comment) {
        if (comment != null && !comment.isBlank() && dialect.supportsTableComments()) {
            String qualifiedName = getQualifiedTableName();
            String escapedComment = SqlEscapeUtil.escapeStringLiteral(comment);
            postStatements.add(dialect.getTableCommentSyntax(qualifiedName, escapedComment));
        }
        return this;
    }

    public CreateTableSqlBuilder columnComment(String columnName, String comment) {
        if (comment != null && !comment.isBlank() && dialect.supportsTableComments()) {
            String qualifiedName = getQualifiedTableName();
            String escapedColumnName = SqlEscapeUtil.escapeIdentifier(columnName);
            String escapedComment = SqlEscapeUtil.escapeStringLiteral(comment);
            postStatements.add(dialect.getColumnCommentSyntax(qualifiedName, escapedColumnName, escapedComment));
        }
        return this;
    }

    /**
     * Returns the main CREATE TABLE statement and any post-statements (like comments).
     */
    public String build() {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException("Table name is required");
        }
        if (definitions.isEmpty()) {
            throw new IllegalStateException("At least one column or constraint definition is required");
        }

        mainStatement.setLength(0);
        mainStatement.append("CREATE TABLE ");

        if (ifNotExists && dialect.supportsIfNotExists()) {
            mainStatement.append(dialect.getIfNotExistsSyntax());
        }

        mainStatement.append(getQualifiedTableName()).append(" (");
        mainStatement.append(System.lineSeparator());

        for (int i = 0; i < definitions.size(); i++) {
            mainStatement.append("  ").append(definitions.get(i));
            if (i < definitions.size() - 1) {
                mainStatement.append(",");
            }
            mainStatement.append(System.lineSeparator());
        }

        mainStatement.append(")");

        // Combine main statement with post-statements
        StringBuilder fullSql = new StringBuilder(mainStatement);
        for (String postStatement : postStatements) {
            fullSql.append(";").append(System.lineSeparator());
            fullSql.append(postStatement);
        }

        return fullSql.toString();
    }

    private String getQualifiedTableName() {
        if (schemaName != null && !schemaName.isBlank()) {
            return SqlEscapeUtil.qualifiedName(schemaName, tableName);
        }
        return SqlEscapeUtil.escapeIdentifier(tableName);
    }

    public List<String> buildStatements() {
        List<String> statements = new ArrayList<>();

        // Build main statement first (without post-statements)
        mainStatement.setLength(0);
        mainStatement.append("CREATE TABLE ");

        if (ifNotExists && dialect.supportsIfNotExists()) {
            mainStatement.append(dialect.getIfNotExistsSyntax());
        }

        mainStatement.append(getQualifiedTableName()).append(" (");
        mainStatement.append(System.lineSeparator());

        for (int i = 0; i < definitions.size(); i++) {
            mainStatement.append("  ").append(definitions.get(i));
            if (i < definitions.size() - 1) {
                mainStatement.append(",");
            }
            mainStatement.append(System.lineSeparator());
        }

        mainStatement.append(")");

        statements.add(mainStatement.toString());
        statements.addAll(postStatements);

        return statements;
    }
}
