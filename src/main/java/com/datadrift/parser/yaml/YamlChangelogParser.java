package com.datadrift.parser.yaml;

import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.parser.ChangeSetLoader;
import com.datadrift.parser.ChangelogParser;
import com.datadrift.parser.ParsedNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class YamlChangelogParser implements ChangelogParser {

    private final ChangeSetLoader changeSetLoader;

    @SuppressWarnings("unchecked")
    @Override
    public List<ChangeSet> parse(File yamlFile) {
        log.info("Parsing YAML changelog: {}", yamlFile.getName());

        Map<String, Object> root = parseYaml(yamlFile);
        List<ChangeSet> changeSets = new ArrayList<>();

        Object changeLogObj = root.get(ELEMENT_DATABASE_CHANGELOG);
        if (changeLogObj == null) {
            throw new RuntimeException("Missing '" + ELEMENT_DATABASE_CHANGELOG + "' root element in: " + yamlFile.getName());
        }

        if (!(changeLogObj instanceof List<?> changeLogList)) {
            throw new RuntimeException("'" + ELEMENT_DATABASE_CHANGELOG + "' must be a list in: " + yamlFile.getName());
        }

        for (Object entry : changeLogList) {
            if (entry instanceof Map<?, ?> entryMap) {

                Map<String, Object> changeSetWrapper = (Map<String, Object>) entryMap;

                if (changeSetWrapper.containsKey(ELEMENT_CHANGESET)) {

                    Map<String, Object> changeSetMap = (Map<String, Object>) changeSetWrapper.get(ELEMENT_CHANGESET);

                    ParsedNode parsedNode = convertToChangeSetNode(changeSetMap);
                    ChangeSet changeSet = changeSetLoader.load(parsedNode);
                    changeSet.setFilename(yamlFile.getName());
                    changeSets.add(changeSet);
                }
            }
        }

        log.info("Parsed {} changeset(s) from {}", changeSets.size(), yamlFile.getName());
        return changeSets;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(File yamlFile) {
        try (InputStream inputStream = new FileInputStream(yamlFile)) {
            Yaml yaml = new Yaml();
            Object result = yaml.load(inputStream);
            if (result == null) {
                throw new RuntimeException("Empty YAML file: " + yamlFile.getName());
            }
            if (!(result instanceof Map)) {
                throw new RuntimeException("YAML root must be a mapping in: " + yamlFile.getName());
            }
            return (Map<String, Object>) result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read YAML file: " + yamlFile.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private ParsedNode convertToChangeSetNode(Map<String, Object> changeSetMap) {
        ParsedNode node = new ParsedNode();
        node.setName(ELEMENT_CHANGESET);

        for (Map.Entry<String, Object> entry : changeSetMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (CHANGESET_ATTRIBUTES.contains(key)) {
                // Store as attribute
                node.getAttributes().put(key, String.valueOf(value));
            } else if (ELEMENT_COMMENT.equals(key)) {
                // Comment becomes a child node with value
                ParsedNode commentNode = new ParsedNode();
                commentNode.setName(ELEMENT_COMMENT);
                commentNode.setValue(String.valueOf(value));
                node.getChildren().add(commentNode);
            } else if (ELEMENT_CHANGES.equals(key)) {
                // Unwrap the changes list - each change becomes a direct child
                if (value instanceof List<?> changesList) {
                    for (Object changeEntry : changesList) {
                        if (changeEntry instanceof Map<?, ?> changeMap) {

                            Map<String, Object> typedChangeMap = (Map<String, Object>) changeMap;
                            // Each change entry is a single-key map like { createTable: {...} }
                            for (Map.Entry<String, Object> changeTypeEntry : typedChangeMap.entrySet()) {
                                ParsedNode changeNode = convertNode(changeTypeEntry.getKey(), changeTypeEntry.getValue());
                                node.getChildren().add(changeNode);
                            }
                        }
                    }
                }
            } else if (ELEMENT_ROLLBACK.equals(key)) {
                // Rollback becomes a child node with its changes as children
                ParsedNode rollbackNode = new ParsedNode();
                rollbackNode.setName(ELEMENT_ROLLBACK);

                if (value instanceof List<?> rollbackList) {
                    for (Object rollbackEntry : rollbackList) {
                        if (rollbackEntry instanceof Map<?, ?> rollbackMap) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> typedRollbackMap = (Map<String, Object>) rollbackMap;
                            for (Map.Entry<String, Object> rollbackTypeEntry : typedRollbackMap.entrySet()) {
                                ParsedNode rollbackChangeNode = convertNode(rollbackTypeEntry.getKey(), rollbackTypeEntry.getValue());
                                rollbackNode.getChildren().add(rollbackChangeNode);
                            }
                        }
                    }
                }
                node.getChildren().add(rollbackNode);
            }
        }

        return node;
    }

    /**
     * Recursively convert a YAML node to a ParsedNode.
     *
     * Mapping rules:
     * - Scalar values become attributes on the parent
     * - Map values with scalar children become nodes with attributes
     * - List values become multiple child nodes
     * - Special text content elements (sql, where) have their content as value
     */
    private ParsedNode convertNode(String name, Object value) {
        ParsedNode node = new ParsedNode();
        node.setName(name);

        if (value == null) {
            return node;
        }

        if (value instanceof Map<?, ?> mapValue) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typedMap = (Map<String, Object>) mapValue;
            processMapValue(node, typedMap);
        } else if (value instanceof List<?> listValue) {
            // This shouldn't typically happen at this level, but handle it
            for (Object item : listValue) {
                if (item instanceof Map<?, ?> itemMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedItemMap = (Map<String, Object>) itemMap;
                    for (Map.Entry<String, Object> itemEntry : typedItemMap.entrySet()) {
                        node.getChildren().add(convertNode(itemEntry.getKey(), itemEntry.getValue()));
                    }
                }
            }
        } else {
            // Scalar value - becomes the node's value
            node.setValue(String.valueOf(value));
        }

        return node;
    }

    /**
     * Process a map value, distinguishing between attributes and children.
     */
    private void processMapValue(ParsedNode node, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            if (isScalarValue(value)) {
                // Check if this is a text content element with same name as parent
                // e.g., sql: { sql: "SELECT 1;" } - the inner sql becomes the value
                if (TEXT_CONTENT_ELEMENTS.contains(key) && key.equals(node.getName())) {
                    node.setValue(String.valueOf(value));
                } else if (TEXT_CONTENT_ELEMENTS.contains(key)) {
                    // Text content element as child (e.g., where clause inside delete)
                    ParsedNode childNode = new ParsedNode();
                    childNode.setName(key);
                    childNode.setValue(String.valueOf(value));
                    node.getChildren().add(childNode);
                } else {
                    // Scalar becomes an attribute
                    node.getAttributes().put(key, String.valueOf(value));
                }
            } else if (value instanceof List<?> listValue) {
                // List becomes multiple children with the singular name
                processListValue(node, key, listValue);
            } else if (value instanceof Map<?, ?> mapValue) {
                // Nested map becomes a child node
                @SuppressWarnings("unchecked")
                Map<String, Object> typedMap = (Map<String, Object>) mapValue;
                ParsedNode childNode = convertNode(key, typedMap);
                node.getChildren().add(childNode);
            }
        }
    }

    /**
     * Process a list value, creating child nodes for each item.
     */
    private void processListValue(ParsedNode parent, String key, List<?> listValue) {
        for (Object item : listValue) {
            if (item instanceof Map<?, ?> itemMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedItemMap = (Map<String, Object>) itemMap;

                // Check if it's a single-key wrapper like { column: {...} }
                if (typedItemMap.size() == 1) {
                    Map.Entry<String, Object> singleEntry = typedItemMap.entrySet().iterator().next();
                    ParsedNode childNode = convertNode(singleEntry.getKey(), singleEntry.getValue());
                    parent.getChildren().add(childNode);
                } else {
                    // Multiple keys - create node with the list key name
                    ParsedNode childNode = convertNode(key, typedItemMap);
                    parent.getChildren().add(childNode);
                }
            } else if (isScalarValue(item)) {
                // Scalar list item - create simple child node
                ParsedNode childNode = new ParsedNode();
                childNode.setName(key);
                childNode.setValue(String.valueOf(item));
                parent.getChildren().add(childNode);
            }
        }
    }

    private boolean isScalarValue(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean;
    }
}
