package com.datadrift.parser;

import com.datadrift.model.change.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertyMapperTest {

    private PropertyMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PropertyMapper();
    }

    @Test
    void testPopulate_SimpleAttributes() {
        ParsedNode node = new ParsedNode();
        node.setName("delete");
        node.getAttributes().put("tableName", "users");
        node.getAttributes().put("schemaName", "public");
        node.getChildren().add(whereChild("id = 1"));

        DeleteChange change = mapper.populate(node, DeleteChange.class);

        assertEquals("users", change.getTableName());
        assertEquals("public", change.getSchemaName());
        assertEquals("id = 1", change.getWhere());
    }

    @Test
    void testPopulate_Alias_CascadeConstraints() {
        ParsedNode node = new ParsedNode();
        node.setName("dropTable");
        node.getAttributes().put("tableName", "users");
        node.getAttributes().put("cascadeConstraints", "true");

        DropTableChange change = mapper.populate(node, DropTableChange.class);

        assertEquals("users", change.getTableName());
        assertTrue(change.getCascade());
    }

    @Test
    void testPopulate_Alias_ColumnNameToColumns() {
        ParsedNode node = new ParsedNode();
        node.setName("dropColumn");
        node.getAttributes().put("tableName", "users");
        node.getAttributes().put("columnName", "role");

        DropColumnChange change = mapper.populate(node, DropColumnChange.class);

        assertEquals("users", change.getTableName());
        assertEquals(List.of("role"), change.getColumns());
    }

    @Test
    void testPopulate_CommaSeparatedList() {
        ParsedNode node = new ParsedNode();
        node.setName("addForeignKey");
        node.getAttributes().put("constraintName", "fk_orders_users");
        node.getAttributes().put("baseTableName", "orders");
        node.getAttributes().put("baseColumnNames", "user_id, org_id");
        node.getAttributes().put("referencedTableName", "users");
        node.getAttributes().put("referencedColumnNames", "id, org_id");

        AddForeignKeyChange change = mapper.populate(node, AddForeignKeyChange.class);

        assertEquals(List.of("user_id", "org_id"), change.getBaseColumnNames());
        assertEquals(List.of("id", "org_id"), change.getReferencedColumnNames());
    }

    @Test
    void testPopulate_Alias_SchemaNames_AddForeignKey() {
        ParsedNode node = new ParsedNode();
        node.setName("addForeignKey");
        node.getAttributes().put("constraintName", "fk_test");
        node.getAttributes().put("baseTableName", "orders");
        node.getAttributes().put("baseColumnNames", "id");
        node.getAttributes().put("referencedTableName", "users");
        node.getAttributes().put("referencedColumnNames", "id");
        node.getAttributes().put("baseTableSchemaName", "public");
        node.getAttributes().put("referencedTableSchemaName", "shared");

        AddForeignKeyChange change = mapper.populate(node, AddForeignKeyChange.class);

        assertEquals("public", change.getBaseSchemaName());
        assertEquals("shared", change.getReferencedSchemaName());
    }

    @Test
    void testPopulate_Alias_SchemaName_DropForeignKey() {
        ParsedNode node = new ParsedNode();
        node.setName("dropForeignKey");
        node.getAttributes().put("constraintName", "fk_orders_users");
        node.getAttributes().put("baseTableName", "orders");
        node.getAttributes().put("baseTableSchemaName", "public");

        DropForeignKeyChange change = mapper.populate(node, DropForeignKeyChange.class);

        assertEquals("public", change.getBaseSchemaName());
    }

    @Test
    void testPopulate_TextContent_Sql() {
        ParsedNode node = new ParsedNode();
        node.setName("sql");
        node.setValue("  CREATE TABLE test (id INT)  ");
        node.getAttributes().put("splitStatements", "true");
        node.getAttributes().put("endDelimiter", ";;");

        SqlChange change = mapper.populate(node, SqlChange.class);

        assertEquals("CREATE TABLE test (id INT)", change.getSql());
        assertTrue(change.isSplitStatements());
        assertEquals(";;", change.getEndDelimiter());
    }

    @Test
    void testPopulate_TextContent_SqlMultiline() {
        ParsedNode node = new ParsedNode();
        node.setName("sql");
        node.setValue("\n            CREATE OR REPLACE FUNCTION test()\n            RETURNS void AS $$ BEGIN END;\n            $$ LANGUAGE plpgsql;\n        ");

        SqlChange change = mapper.populate(node, SqlChange.class);

        assertTrue(change.getSql().startsWith("CREATE OR REPLACE FUNCTION"));
        assertTrue(change.getSql().endsWith("LANGUAGE plpgsql;"));
    }

    @Test
    void testPopulate_WhereChild_Delete() {
        ParsedNode node = new ParsedNode();
        node.setName("delete");
        node.getAttributes().put("tableName", "users");
        node.getChildren().add(whereChild("id > 100"));

        DeleteChange change = mapper.populate(node, DeleteChange.class);

        assertEquals("id > 100", change.getWhere());
    }

    @Test
    void testPopulate_WhereChild_Update_Trimmed() {
        ParsedNode node = new ParsedNode();
        node.setName("update");
        node.getAttributes().put("tableName", "users");
        node.getChildren().add(columnValueChild("name", "value", "Jane"));
        node.getChildren().add(whereChild("  status = 'active'  "));

        UpdateChange change = mapper.populate(node, UpdateChange.class);

        assertEquals("status = 'active'", change.getWhere());
    }

    @Test
    void testPopulate_Columns_ListOfStrings() {
        ParsedNode node = new ParsedNode();
        node.setName("createIndex");
        node.getAttributes().put("indexName", "idx_users_email");
        node.getAttributes().put("tableName", "users");
        node.getAttributes().put("unique", "true");
        node.getChildren().add(columnNameChild("email"));
        node.getChildren().add(columnNameChild("username"));

        CreateIndexChange change = mapper.populate(node, CreateIndexChange.class);

        assertEquals(List.of("email", "username"), change.getColumns());
        assertTrue(change.getUnique());
    }

    @Test
    void testPopulate_ColumnValue_String() {
        ParsedNode node = new ParsedNode();
        node.setName("insert");
        node.getAttributes().put("tableName", "users");
        node.getChildren().add(columnValueChild("name", "value", "John"));

        InsertChange change = mapper.populate(node, InsertChange.class);

        assertEquals("name", change.getColumns().get(0).getName());
        assertEquals("John", change.getColumns().get(0).getValue());
        assertEquals("STRING", change.getColumns().get(0).getValueType());
    }

    @Test
    void testPopulate_ColumnValue_Numeric() {
        ParsedNode node = new ParsedNode();
        node.setName("insert");
        node.getAttributes().put("tableName", "users");
        node.getChildren().add(columnValueChild("age", "valueNumeric", "25"));

        InsertChange change = mapper.populate(node, InsertChange.class);

        assertEquals("25", change.getColumns().get(0).getValue());
        assertEquals("NUMERIC", change.getColumns().get(0).getValueType());
    }

    @Test
    void testPopulate_ColumnValue_Boolean() {
        ParsedNode node = new ParsedNode();
        node.setName("insert");
        node.getAttributes().put("tableName", "users");
        node.getChildren().add(columnValueChild("active", "valueBoolean", "true"));

        InsertChange change = mapper.populate(node, InsertChange.class);

        assertEquals("true", change.getColumns().get(0).getValue());
        assertEquals("BOOLEAN", change.getColumns().get(0).getValueType());
    }

    @Test
    void testPopulate_ColumnValue_Date() {
        ParsedNode node = new ParsedNode();
        node.setName("insert");
        node.getAttributes().put("tableName", "users");
        node.getChildren().add(columnValueChild("created_at", "valueDate", "2024-01-15"));

        InsertChange change = mapper.populate(node, InsertChange.class);

        assertEquals("2024-01-15", change.getColumns().get(0).getValue());
        assertEquals("DATE", change.getColumns().get(0).getValueType());
    }

    @Test
    void testPopulate_ColumnValue_Computed() {
        ParsedNode node = new ParsedNode();
        node.setName("insert");
        node.getAttributes().put("tableName", "users");
        node.getChildren().add(columnValueChild("updated_at", "valueComputed", "CURRENT_TIMESTAMP"));

        InsertChange change = mapper.populate(node, InsertChange.class);

        assertEquals("CURRENT_TIMESTAMP", change.getColumns().get(0).getValue());
        assertEquals("TIMESTAMP", change.getColumns().get(0).getValueType());
    }

    @Test
    void testPopulate_ColumnValue_Null() {
        ParsedNode node = new ParsedNode();
        node.setName("insert");
        node.getAttributes().put("tableName", "users");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "middleName");
        // no value attribute at all
        node.getChildren().add(col);

        InsertChange change = mapper.populate(node, InsertChange.class);

        assertNull(change.getColumns().get(0).getValue());
        assertEquals("NULL", change.getColumns().get(0).getValueType());
    }

    @Test
    void testPopulate_ColumnConfig_Simple() {
        ParsedNode node = new ParsedNode();
        node.setName("createTable");
        node.getAttributes().put("tableName", "users");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "id");
        col.getAttributes().put("type", "BIGSERIAL");
        node.getChildren().add(col);

        CreateTableChange change = mapper.populate(node, CreateTableChange.class);

        assertEquals(1, change.getColumns().size());
        assertEquals("id", change.getColumns().get(0).getName());
        assertEquals("BIGSERIAL", change.getColumns().get(0).getType());
    }

    @Test
    void testPopulate_ColumnConfig_WithPrimaryKeyConstraint() {
        ParsedNode node = new ParsedNode();
        node.setName("createTable");
        node.getAttributes().put("tableName", "users");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "id");
        col.getAttributes().put("type", "BIGSERIAL");

        ParsedNode constraints = new ParsedNode();
        constraints.setName("constraints");
        constraints.getAttributes().put("primaryKey", "true");
        constraints.getAttributes().put("nullable", "false");
        col.getChildren().add(constraints);

        node.getChildren().add(col);

        CreateTableChange change = mapper.populate(node, CreateTableChange.class);

        CreateTableChange.ConstraintsConfig c = change.getColumns().get(0).getConstraints();
        assertNotNull(c);
        assertTrue(c.isPrimaryKey());
        assertFalse(c.nullable());
    }

    @Test
    void testPopulate_ColumnConfig_WithUniqueConstraint() {
        ParsedNode node = new ParsedNode();
        node.setName("createTable");
        node.getAttributes().put("tableName", "users");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "email");
        col.getAttributes().put("type", "VARCHAR(255)");

        ParsedNode constraints = new ParsedNode();
        constraints.setName("constraints");
        constraints.getAttributes().put("nullable", "false");
        constraints.getAttributes().put("unique", "true");
        col.getChildren().add(constraints);

        node.getChildren().add(col);

        CreateTableChange change = mapper.populate(node, CreateTableChange.class);

        CreateTableChange.ConstraintsConfig c = change.getColumns().get(0).getConstraints();
        assertFalse(c.nullable());
        assertTrue(c.isUnique());
    }

    @Test
    void testPopulate_ColumnConfig_WithForeignKeyConstraint() {
        ParsedNode node = new ParsedNode();
        node.setName("createTable");
        node.getAttributes().put("tableName", "orders");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "user_id");
        col.getAttributes().put("type", "BIGINT");

        ParsedNode constraints = new ParsedNode();
        constraints.setName("constraints");
        constraints.getAttributes().put("nullable", "false");
        constraints.getAttributes().put("foreignKeyName", "fk_orders_users");
        constraints.getAttributes().put("references", "users(id)");
        col.getChildren().add(constraints);

        node.getChildren().add(col);

        CreateTableChange change = mapper.populate(node, CreateTableChange.class);

        CreateTableChange.ConstraintsConfig c = change.getColumns().get(0).getConstraints();
        assertFalse(c.nullable());
        assertEquals("fk_orders_users", c.foreignKeyName());
        assertEquals("users(id)", c.references());
    }

    @Test
    void testPopulate_ColumnConfig_DefaultValueComputed() {
        ParsedNode node = new ParsedNode();
        node.setName("createTable");
        node.getAttributes().put("tableName", "users");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "created_at");
        col.getAttributes().put("type", "TIMESTAMP");
        col.getAttributes().put("defaultValueComputed", "CURRENT_TIMESTAMP");
        node.getChildren().add(col);

        CreateTableChange change = mapper.populate(node, CreateTableChange.class);

        assertEquals("CURRENT_TIMESTAMP", change.getColumns().get(0).getDefaultValueComputed());
    }

    @Test
    void testPopulate_ColumnConfig_DefaultValueBoolean() {
        ParsedNode node = new ParsedNode();
        node.setName("createTable");
        node.getAttributes().put("tableName", "users");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "is_active");
        col.getAttributes().put("type", "BOOLEAN");
        col.getAttributes().put("defaultValueBoolean", "true");
        node.getChildren().add(col);

        CreateTableChange change = mapper.populate(node, CreateTableChange.class);

        assertEquals("true", change.getColumns().get(0).getDefaultValue());
    }

    @Test
    void testPopulate_ColumnConfig_DefaultValueString() {
        ParsedNode node = new ParsedNode();
        node.setName("createTable");
        node.getAttributes().put("tableName", "users");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "role");
        col.getAttributes().put("type", "VARCHAR(50)");
        col.getAttributes().put("defaultValue", "USER");
        node.getChildren().add(col);

        CreateTableChange change = mapper.populate(node, CreateTableChange.class);

        assertEquals("USER", change.getColumns().get(0).getDefaultValue());
    }

    @Test
    void testPopulate_ColumnConfig_MultipleColumns() {
        ParsedNode node = new ParsedNode();
        node.setName("createTable");
        node.getAttributes().put("tableName", "users");

        for (String name : new String[]{"id", "name", "email"}) {
            ParsedNode col = new ParsedNode();
            col.setName("column");
            col.getAttributes().put("name", name);
            col.getAttributes().put("type", "TEXT");
            node.getChildren().add(col);
        }

        CreateTableChange change = mapper.populate(node, CreateTableChange.class);

        assertEquals(3, change.getColumns().size());
        assertEquals("id", change.getColumns().get(0).getName());
        assertEquals("name", change.getColumns().get(1).getName());
        assertEquals("email", change.getColumns().get(2).getName());
    }

    @Test
    void testPopulate_AddColumn_ColumnConfig() {
        ParsedNode node = new ParsedNode();
        node.setName("addColumn");
        node.getAttributes().put("tableName", "users");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "role");
        col.getAttributes().put("type", "VARCHAR(50)");
        col.getAttributes().put("defaultValue", "USER");

        ParsedNode constraints = new ParsedNode();
        constraints.setName("constraints");
        constraints.getAttributes().put("nullable", "false");
        col.getChildren().add(constraints);

        node.getChildren().add(col);

        AddColumnChange change = mapper.populate(node, AddColumnChange.class);

        assertEquals("users", change.getTableName());
        assertEquals("role", change.getColumns().get(0).getName());
        assertEquals("USER", change.getColumns().get(0).getDefaultValue());
        assertFalse(change.getColumns().get(0).getConstraints().nullable());
    }

    @Test
    void testPopulate_Update_ColumnsAndWhere() {
        ParsedNode node = new ParsedNode();
        node.setName("update");
        node.getAttributes().put("tableName", "users");
        node.getChildren().add(columnValueChild("name", "value", "Jane"));
        node.getChildren().add(columnValueChild("age", "valueNumeric", "30"));
        node.getChildren().add(whereChild("id = 1"));

        UpdateChange change = mapper.populate(node, UpdateChange.class);

        assertEquals("users", change.getTableName());
        assertEquals(2, change.getColumns().size());
        assertEquals("Jane", change.getColumns().get(0).getValue());
        assertEquals("STRING", change.getColumns().get(0).getValueType());
        assertEquals("30", change.getColumns().get(1).getValue());
        assertEquals("NUMERIC", change.getColumns().get(1).getValueType());
        assertEquals("id = 1", change.getWhere());
    }

    @Test
    void testPopulate_UnknownAttributesSkipped() {
        ParsedNode node = new ParsedNode();
        node.setName("delete");
        node.getAttributes().put("tableName", "users");
        node.getAttributes().put("unknownAttr", "someValue");
        node.getChildren().add(whereChild("id = 1"));

        DeleteChange change = mapper.populate(node, DeleteChange.class);

        assertEquals("users", change.getTableName());
        assertEquals("id = 1", change.getWhere());
    }

    @Test
    void testPopulate_DropIndex() {
        ParsedNode node = new ParsedNode();
        node.setName("dropIndex");
        node.getAttributes().put("indexName", "idx_users_email");
        node.getAttributes().put("schemaName", "public");

        DropIndexChange change = mapper.populate(node, DropIndexChange.class);

        assertEquals("idx_users_email", change.getIndexName());
        assertEquals("public", change.getSchemaName());
    }


    @Test
    void testPopulate_ColumnConfig_NoConstraints() {
        ParsedNode node = new ParsedNode();
        node.setName("createTable");
        node.getAttributes().put("tableName", "users");

        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", "nickname");
        col.getAttributes().put("type", "VARCHAR(50)");
        // no <constraints> child
        node.getChildren().add(col);

        CreateTableChange change = mapper.populate(node, CreateTableChange.class);

        assertNull(change.getColumns().get(0).getConstraints());
    }

    private ParsedNode whereChild(String value) {
        ParsedNode where = new ParsedNode();
        where.setName("where");
        where.setValue(value);
        return where;
    }

    private ParsedNode columnValueChild(String name, String valueAttr, String value) {
        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", name);
        col.getAttributes().put(valueAttr, value);
        return col;
    }

    private ParsedNode columnNameChild(String name) {
        ParsedNode col = new ParsedNode();
        col.setName("column");
        col.getAttributes().put("name", name);
        return col;
    }
}
