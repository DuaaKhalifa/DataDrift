package com.datadrift.parser;

import com.datadrift.model.change.DropTableChange;
import com.datadrift.model.change.SqlChange;
import com.datadrift.model.changelog.ChangeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeSetLoaderTest {

    @Mock
    private PropertyMapper propertyMapper;

    private ChangeSetLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ChangeSetLoader(propertyMapper);
    }

    @Test
    void testLoad_BasicAttributes() {
        ParsedNode node = changeSetNode("cs-001", "alice");

        ChangeSet result = loader.load(node);

        assertEquals("cs-001", result.getId());
        assertEquals("alice", result.getAuthor());
    }

    @Test
    void testLoad_ContextAndLabels() {
        ParsedNode node = changeSetNode("cs-001", "alice");
        node.getAttributes().put("context", "production");
        node.getAttributes().put("labels", "schema,data");

        ChangeSet result = loader.load(node);

        assertEquals("production", result.getContext());
        assertEquals("schema,data", result.getLabels());
    }

    @Test
    void testLoad_FailOnError_DefaultsToTrue() {
        ParsedNode node = changeSetNode("cs-001", "alice");

        ChangeSet result = loader.load(node);

        assertTrue(result.isFailOnError());
    }

    @Test
    void testLoad_RunAlways_DefaultsToFalse() {
        ParsedNode node = changeSetNode("cs-001", "alice");

        ChangeSet result = loader.load(node);

        assertFalse(result.isRunAlways());
    }

    @Test
    void testLoad_CommentExtracted() {
        ParsedNode node = changeSetNode("cs-001", "alice");
        node.getChildren().add(commentNode("Add users table"));

        ChangeSet result = loader.load(node);

        assertEquals("Add users table", result.getComment());
    }

    @Test
    void testLoad_CommentTrimmed() {
        ParsedNode node = changeSetNode("cs-001", "alice");
        node.getChildren().add(commentNode("  Add users table  "));

        ChangeSet result = loader.load(node);

        assertEquals("Add users table", result.getComment());
    }

    @Test
    void testLoad_SingleChange() {
        ParsedNode node = changeSetNode("cs-001", "alice");
        ParsedNode dropNode = changeNode("dropTable", Map.of("tableName", "users"));
        node.getChildren().add(dropNode);

        DropTableChange mockChange = new DropTableChange();
        mockChange.setTableName("users");
        when(propertyMapper.populate(eq(dropNode), eq(DropTableChange.class))).thenReturn(mockChange);

        ChangeSet result = loader.load(node);

        assertEquals(1, result.getChanges().size());
        assertEquals("users", ((DropTableChange) result.getChanges().get(0)).getTableName());
        verify(propertyMapper).populate(dropNode, DropTableChange.class);
    }

    @Test
    void testLoad_MultipleChanges() {
        ParsedNode node = changeSetNode("cs-001", "alice");

        ParsedNode dropNode = changeNode("dropTable", Map.of("tableName", "users"));
        ParsedNode sqlNode = new ParsedNode();
        sqlNode.setName("sql");
        sqlNode.setValue("SELECT 1");

        node.getChildren().add(dropNode);
        node.getChildren().add(sqlNode);

        DropTableChange mockDrop = new DropTableChange();
        mockDrop.setTableName("users");
        when(propertyMapper.populate(eq(dropNode), eq(DropTableChange.class))).thenReturn(mockDrop);

        SqlChange mockSql = new SqlChange();
        mockSql.setSql("SELECT 1");
        when(propertyMapper.populate(eq(sqlNode), eq(SqlChange.class))).thenReturn(mockSql);

        ChangeSet result = loader.load(node);

        assertEquals(2, result.getChanges().size());
        assertInstanceOf(DropTableChange.class, result.getChanges().get(0));
        assertInstanceOf(SqlChange.class, result.getChanges().get(1));
    }

    @Test
    void testLoad_NoChanges_EmptyList() {
        ParsedNode node = changeSetNode("cs-001", "alice");

        ChangeSet result = loader.load(node);

        assertNotNull(result.getChanges());
        assertTrue(result.getChanges().isEmpty());
    }

    @Test
    void testLoad_RollbackWithSingleChange() {
        ParsedNode node = changeSetNode("cs-001", "alice");

        ParsedNode dropNode = changeNode("dropTable", Map.of("tableName", "users"));
        node.getChildren().add(dropNode);

        ParsedNode rollbackNode = new ParsedNode();
        rollbackNode.setName("rollback");
        ParsedNode rollbackDropNode = changeNode("dropTable", Map.of("tableName", "old_users"));
        rollbackNode.getChildren().add(rollbackDropNode);
        node.getChildren().add(rollbackNode);

        DropTableChange mockChange = new DropTableChange();
        mockChange.setTableName("users");
        when(propertyMapper.populate(eq(dropNode), eq(DropTableChange.class))).thenReturn(mockChange);

        DropTableChange mockRollback = new DropTableChange();
        mockRollback.setTableName("old_users");
        when(propertyMapper.populate(eq(rollbackDropNode), eq(DropTableChange.class))).thenReturn(mockRollback);

        ChangeSet result = loader.load(node);

        assertEquals(1, result.getChanges().size());
        assertEquals(1, result.getRollbackChanges().size());
        assertEquals("old_users", ((DropTableChange) result.getRollbackChanges().get(0)).getTableName());
    }

    @Test
    void testLoad_RollbackWithMultipleChanges() {
        ParsedNode node = changeSetNode("cs-001", "alice");

        ParsedNode rollbackNode = new ParsedNode();
        rollbackNode.setName("rollback");

        ParsedNode rb1 = changeNode("dropTable", Map.of("tableName", "t1"));
        ParsedNode rb2 = changeNode("dropTable", Map.of("tableName", "t2"));
        rollbackNode.getChildren().add(rb1);
        rollbackNode.getChildren().add(rb2);
        node.getChildren().add(rollbackNode);

        DropTableChange mock1 = new DropTableChange();
        mock1.setTableName("t1");
        when(propertyMapper.populate(eq(rb1), eq(DropTableChange.class))).thenReturn(mock1);

        DropTableChange mock2 = new DropTableChange();
        mock2.setTableName("t2");
        when(propertyMapper.populate(eq(rb2), eq(DropTableChange.class))).thenReturn(mock2);

        ChangeSet result = loader.load(node);

        assertEquals(2, result.getRollbackChanges().size());
    }

    @Test
    void testLoad_UnknownChangeType_ThrowsException() {
        ParsedNode node = changeSetNode("cs-001", "alice");
        node.getChildren().add(changeNode("nonExistentFoo", Map.of("bar", "baz")));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> loader.load(node)
        );
        assertTrue(ex.getMessage().contains("Unknown change type: nonExistentFoo"));
    }

    @Test
    void testLoad_FullChangeSet() {
        ParsedNode node = changeSetNode("cs-full", "bob");
        node.getAttributes().put("context", "staging");
        node.getAttributes().put("labels", "schema");
        node.getAttributes().put("runAlways", "true");
        node.getAttributes().put("failOnError", "false");

        node.getChildren().add(commentNode("Full test changeset"));

        ParsedNode dropNode = changeNode("dropTable", Map.of("tableName", "legacy"));
        node.getChildren().add(dropNode);

        ParsedNode rollbackNode = new ParsedNode();
        rollbackNode.setName("rollback");
        ParsedNode rbSqlNode = new ParsedNode();
        rbSqlNode.setName("sql");
        rbSqlNode.setValue("CREATE TABLE legacy (id int)");
        rollbackNode.getChildren().add(rbSqlNode);
        node.getChildren().add(rollbackNode);

        DropTableChange mockDrop = new DropTableChange();
        mockDrop.setTableName("legacy");
        when(propertyMapper.populate(eq(dropNode), eq(DropTableChange.class))).thenReturn(mockDrop);

        SqlChange mockRbSql = new SqlChange();
        mockRbSql.setSql("CREATE TABLE legacy (id int)");
        when(propertyMapper.populate(eq(rbSqlNode), eq(SqlChange.class))).thenReturn(mockRbSql);

        ChangeSet result = loader.load(node);

        assertEquals("cs-full", result.getId());
        assertEquals("bob", result.getAuthor());
        assertEquals("staging", result.getContext());
        assertEquals("schema", result.getLabels());
        assertTrue(result.isRunAlways());
        assertFalse(result.isFailOnError());
        assertEquals("Full test changeset", result.getComment());
        assertEquals(1, result.getChanges().size());
        assertEquals(1, result.getRollbackChanges().size());
    }

    private ParsedNode changeSetNode(String id, String author) {
        ParsedNode node = new ParsedNode();
        node.setName("changeSet");
        node.getAttributes().put("id", id);
        node.getAttributes().put("author", author);
        return node;
    }

    private ParsedNode commentNode(String text) {
        ParsedNode node = new ParsedNode();
        node.setName("comment");
        node.setValue(text);
        return node;
    }

    private ParsedNode changeNode(String tagName, Map<String, String> attrs) {
        ParsedNode node = new ParsedNode();
        node.setName(tagName);
        node.getAttributes().putAll(attrs);
        return node;
    }
}
