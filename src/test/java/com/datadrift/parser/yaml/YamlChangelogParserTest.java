package com.datadrift.parser.yaml;

import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.parser.ChangeSetLoader;
import com.datadrift.parser.ParsedNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YamlChangelogParserTest {

    @Mock
    private ChangeSetLoader changeSetLoader;

    private YamlChangelogParser parser;

    @BeforeEach
    void setUp() {
        parser = new YamlChangelogParser(changeSetLoader);
    }

    private File resourceFile(String name) {
        return new File(getClass().getClassLoader().getResource("yaml/" + name).getFile());
    }

    @Test
    void testParse_SingleChangeset_ReturnsOneChangeSet() {
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        List<ChangeSet> result = parser.parse(resourceFile("test-single-changeset.yaml"));

        assertEquals(1, result.size());
        verify(changeSetLoader, times(1)).load(any());
    }

    @Test
    void testParse_SingleChangeset_NodeNameIsChangeSet() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-single-changeset.yaml"));

        verify(changeSetLoader).load(captor.capture());
        assertEquals("changeSet", captor.getValue().getName());
    }

    @Test
    void testParse_SingleChangeset_AttributesParsed() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-single-changeset.yaml"));

        verify(changeSetLoader).load(captor.capture());
        assertEquals("001", captor.getValue().getAttributes().get("id"));
        assertEquals("test", captor.getValue().getAttributes().get("author"));
    }

    @Test
    void testParse_SingleChangeset_CommentChildExtracted() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-single-changeset.yaml"));

        verify(changeSetLoader).load(captor.capture());
        ParsedNode comment = captor.getValue().getChildren().stream()
                .filter(c -> "comment".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("Test comment", comment.getValue());
    }

    @Test
    void testParse_SingleChangeset_ChangeChildExtracted() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-single-changeset.yaml"));

        verify(changeSetLoader).load(captor.capture());
        ParsedNode dropTable = captor.getValue().getChildren().stream()
                .filter(c -> "dropTable".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("users", dropTable.getAttributes().get("tableName"));
    }

    @Test
    void testParse_MultipleChangesets_CallsLoaderForEach() {
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        List<ChangeSet> result = parser.parse(resourceFile("test-multiple-changesets.yaml"));

        assertEquals(2, result.size());
        verify(changeSetLoader, times(2)).load(any());
    }

    @Test
    void testParse_MultipleChangesets_PreservesDocumentOrder() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-multiple-changesets.yaml"));

        verify(changeSetLoader, times(2)).load(captor.capture());
        List<ParsedNode> nodes = captor.getAllValues();
        assertEquals("001", nodes.get(0).getAttributes().get("id"));
        assertEquals("002", nodes.get(1).getAttributes().get("id"));
    }

    @Test
    void testParse_WithRollback_RollbackChildExists() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-with-rollback.yaml"));

        verify(changeSetLoader).load(captor.capture());
        ParsedNode rollback = captor.getValue().getChildren().stream()
                .filter(c -> "rollback".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertFalse(rollback.getChildren().isEmpty());
    }

    @Test
    void testParse_WithRollback_RollbackContainsSqlWithValue() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-with-rollback.yaml"));

        verify(changeSetLoader).load(captor.capture());
        ParsedNode rollback = captor.getValue().getChildren().stream()
                .filter(c -> "rollback".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        ParsedNode sql = rollback.getChildren().get(0);
        assertEquals("sql", sql.getName());
        assertEquals("CREATE TABLE users (id int);", sql.getValue());
    }

    @Test
    void testParse_SqlTextContent_ValuePreserved() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-sql-content.yaml"));

        verify(changeSetLoader).load(captor.capture());
        ParsedNode sql = captor.getValue().getChildren().stream()
                .filter(c -> "sql".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("SELECT 1;", sql.getValue());
    }

    @Test
    void testParse_NestedElements_ColumnInsideCreateTable() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-nested-elements.yaml"));

        verify(changeSetLoader).load(captor.capture());
        ParsedNode createTable = captor.getValue().getChildren().stream()
                .filter(c -> "createTable".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("users", createTable.getAttributes().get("tableName"));
        assertEquals(1, createTable.getChildren().size());

        ParsedNode column = createTable.getChildren().get(0);
        assertEquals("column", column.getName());
        assertEquals("id", column.getAttributes().get("name"));
        assertEquals("BIGINT", column.getAttributes().get("type"));
    }

    @Test
    void testParse_NestedElements_ConstraintsInsideColumn() {
        ArgumentCaptor<ParsedNode> captor = ArgumentCaptor.forClass(ParsedNode.class);
        when(changeSetLoader.load(any())).thenReturn(new ChangeSet());

        parser.parse(resourceFile("test-nested-elements.yaml"));

        verify(changeSetLoader).load(captor.capture());
        ParsedNode column = captor.getValue().getChildren().stream()
                .filter(c -> "createTable".equals(c.getName()))
                .findFirst()
                .orElseThrow()
                .getChildren().get(0);

        assertEquals(1, column.getChildren().size());
        ParsedNode constraints = column.getChildren().get(0);
        assertEquals("constraints", constraints.getName());
        assertEquals("true", constraints.getAttributes().get("primaryKey"));
        assertEquals("false", constraints.getAttributes().get("nullable"));
    }

    @Test
    void testParse_SetsFilenameOnChangeSet() {
        ChangeSet mockCS = new ChangeSet();
        when(changeSetLoader.load(any())).thenReturn(mockCS);

        parser.parse(resourceFile("test-single-changeset.yaml"));

        assertEquals("test-single-changeset.yaml", mockCS.getFilename());
    }

    @Test
    void testParse_SetsFilenameOnAllChangesets() {
        ChangeSet cs1 = new ChangeSet();
        ChangeSet cs2 = new ChangeSet();
        when(changeSetLoader.load(any())).thenReturn(cs1).thenReturn(cs2);

        parser.parse(resourceFile("test-multiple-changesets.yaml"));

        assertEquals("test-multiple-changesets.yaml", cs1.getFilename());
        assertEquals("test-multiple-changesets.yaml", cs2.getFilename());
    }

    @Test
    void testParse_EmptyChangelog_ReturnsEmptyList() {
        List<ChangeSet> result = parser.parse(resourceFile("test-empty.yaml"));

        assertTrue(result.isEmpty());
        verifyNoInteractions(changeSetLoader);
    }

    @Test
    void testParse_MalformedYaml_ThrowsException() {
        assertThrows(RuntimeException.class, () ->
                parser.parse(resourceFile("test-malformed.yaml"))
        );
    }

    @Test
    void testParse_MissingDatabaseChangeLog_ThrowsException() {
        File file = new File(getClass().getClassLoader().getResource("yaml/test-missing-root.yaml").getFile());
        assertThrows(RuntimeException.class, () -> parser.parse(file));
    }
}
