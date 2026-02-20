package com.datadrift.parser;

import com.datadrift.model.change.Change;
import com.datadrift.model.changelog.ChangeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChangeSetLoader {

    private static final String CHANGE_CLASS_PREFIX = "com.datadrift.model.change.";

    private final PropertyMapper propertyMapper;

    public ChangeSet load(ParsedNode changeSetNode) {
        Map<String, String> attrs = changeSetNode.getAttributes();

        ChangeSet changeSet = new ChangeSet();
        changeSet.setId(attrs.get("id"));
        changeSet.setAuthor(attrs.get("author"));
        changeSet.setTag(attrs.get("tag"));
        changeSet.setContext(attrs.get("context"));
        changeSet.setLabels(attrs.get("labels"));

        if (attrs.containsKey("runAlways")) {
            changeSet.setRunAlways(Boolean.parseBoolean(attrs.get("runAlways")));
        }
        if (attrs.containsKey("runOnChange")) {
            changeSet.setRunOnChange(Boolean.parseBoolean(attrs.get("runOnChange")));
        }
        if (attrs.containsKey("failOnError")) {
            changeSet.setFailOnError(Boolean.parseBoolean(attrs.get("failOnError")));
        }

        List<Change> changes = new ArrayList<>();
        List<Change> rollbackChanges = new ArrayList<>();

        for (ParsedNode child : changeSetNode.getChildren()) {
            if ("comment".equals(child.getName())) {
                changeSet.setComment(child.getValue() != null ? child.getValue().trim() : null);
            } else if ("rollback".equals(child.getName())) {
                rollbackChanges.addAll(loadChanges(child.getChildren()));
            } else {
                changes.addAll(loadChanges(List.of(child)));
            }
        }

        changeSet.setChanges(changes);
        changeSet.setRollbackChanges(rollbackChanges);

        return changeSet;
    }

    private List<Change> loadChanges(List<ParsedNode> nodes) {
        List<Change> changes = new ArrayList<>();
        for (ParsedNode node : nodes) {
            Class<? extends Change> changeClass = resolveChangeClass(node.getName());
            changes.add(propertyMapper.populate(node, changeClass));
        }
        return changes;
    }

    /**
     * Resolves XML tag name to Change class using naming convention:
     * - Tag name: "createTable" → Class name: "CreateTableChange"
     * - Tag name: "dropTable" → Class name: "DropTableChange"
     * - Tag name: "addColumn" → Class name: "AddColumnChange"
     *
     * Convention: capitalize(tagName) + "Change"
     *
     * This eliminates the need for a ChangeFactory registry — as long as the
     * class name matches the tag name, it's automatically discoverable via reflection.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Change> resolveChangeClass(String tagName) {
        String className = CHANGE_CLASS_PREFIX
                + tagName.substring(0, 1).toUpperCase()
                + tagName.substring(1)
                + "Change";
        try {
            Class<?> clazz = Class.forName(className);
            if (!Change.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class " + className + " does not implement Change");
            }
            return (Class<? extends Change>) clazz;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown change type: " + tagName);
        }
    }
}
