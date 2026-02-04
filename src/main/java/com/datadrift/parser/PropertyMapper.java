package com.datadrift.parser;

import com.datadrift.model.change.Change;
import com.datadrift.model.change.CreateTableChange;
import com.datadrift.model.change.ColumnValue;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class PropertyMapper {

    /**
     * XSD attribute names that differ from the Java field names.
     */
    private static final Map<String, String> ATTRIBUTE_ALIASES = Map.of(
            "cascadeConstraints", "cascade",
            "columnName", "columns",
            "baseTableSchemaName", "baseSchemaName",
            "referencedTableSchemaName", "referencedSchemaName",
            "defaultValueBoolean", "defaultValue",
            "defaultValueNumeric", "defaultValue"
    );

    /**
     * Populate a Change instance from a ParsedNode using reflection.
     * Unknown attributes (no matching setter) are silently skipped;
     * validation of required fields is left to Change.validate().
     */
    public <T extends Change> T populate(ParsedNode node, Class<T> clazz) {
        T instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getSimpleName(), e);
        }

        setAttributes(instance, node.getAttributes());
        setTextContent(instance, node);
        processChildren(instance, node.getChildren());
        return instance;
    }

    private void setAttributes(Object instance, Map<String, String> attributes) {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            setProperty(instance, resolveAlias(entry.getKey()), entry.getValue());
        }
    }

    private void setTextContent(Object instance, ParsedNode node) {
        if (node.getValue() != null && !node.getValue().isBlank()) {
            setProperty(instance, node.getName(), node.getValue().trim());
        }
    }

    private void processChildren(Object instance, List<ParsedNode> children) {
        List<ParsedNode> columnNodes = new ArrayList<>();

        for (ParsedNode child : children) {
            if ("column".equals(child.getName())) {
                columnNodes.add(child);
            } else if (child.getValue() != null) {
                // Text-content children like <where>id = 1</where>
                setProperty(instance, child.getName(), child.getValue().trim());
            }
        }

        if (!columnNodes.isEmpty()) {
            populateColumns(instance, columnNodes);
        }
    }

    private void populateColumns(Object instance, List<ParsedNode> columnNodes) {
        Method setter = findSetter(instance.getClass(), "columns");
        if (setter == null) return;

        Class<?> elementType = getListElementType(setter.getGenericParameterTypes()[0]);
        List<?> columns;

        if (elementType == CreateTableChange.ColumnConfig.class) {
            columns = columnNodes.stream().map(this::populateColumnConfig).toList();
        } else if (elementType == ColumnValue.class) {
            columns = columnNodes.stream().map(this::populateColumnValue).toList();
        } else {
            // List<String> — extract name attribute
            columns = columnNodes.stream()
                    .map(n -> n.getAttributes().get("name"))
                    .toList();
        }

        invokeMethod(setter, instance, columns);
    }

    private CreateTableChange.ColumnConfig populateColumnConfig(ParsedNode node) {
        CreateTableChange.ColumnConfig config = new CreateTableChange.ColumnConfig();
        setAttributes(config, node.getAttributes());

        node.getChildren().stream()
                .filter(c -> "constraints".equals(c.getName()))
                .findFirst()
                .ifPresent(n -> config.setConstraints(populateConstraints(n)));

        return config;
    }

    private ColumnValue populateColumnValue(ParsedNode node) {
        ColumnValue cv = new ColumnValue();
        Map<String, String> attrs = node.getAttributes();
        cv.setName(attrs.get("name"));

        if (attrs.containsKey("value")) {
            cv.setValue(attrs.get("value"));
            cv.setValueType("STRING");
        } else if (attrs.containsKey("valueNumeric")) {
            cv.setValue(attrs.get("valueNumeric"));
            cv.setValueType("NUMERIC");
        } else if (attrs.containsKey("valueBoolean")) {
            cv.setValue(attrs.get("valueBoolean"));
            cv.setValueType("BOOLEAN");
        } else if (attrs.containsKey("valueDate")) {
            cv.setValue(attrs.get("valueDate"));
            cv.setValueType("DATE");
        } else if (attrs.containsKey("valueComputed")) {
            cv.setValue(attrs.get("valueComputed"));
            cv.setValueType("TIMESTAMP");
        } else {
            cv.setValueType("NULL");
        }

        return cv;
    }

    private CreateTableChange.ConstraintsConfig populateConstraints(ParsedNode node) {
        Map<String, String> attrs = node.getAttributes();
        return new CreateTableChange.ConstraintsConfig(
                parseBoolean(attrs.get("nullable")),
                parseBoolean(attrs.get("primaryKey")),
                parseBoolean(attrs.get("unique")),
                attrs.get("foreignKeyName"),
                attrs.get("references"),
                attrs.get("checkConstraint"),
                parseForeignKeyAction(attrs.get("onDelete")),
                parseForeignKeyAction(attrs.get("onUpdate"))
        );
    }

    private String resolveAlias(String name) {
        return ATTRIBUTE_ALIASES.getOrDefault(name, name);
    }

    private void setProperty(Object instance, String fieldName, String value) {
        Method setter = findSetter(instance.getClass(), fieldName);
        if (setter == null) return; // no matching setter — silently skip

        Class<?> paramType = setter.getParameterTypes()[0];
        invokeMethod(setter, instance, convertValue(value, paramType));
    }

    private Method findSetter(Class<?> clazz, String fieldName) {
        String name = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        return Arrays.stream(clazz.getMethods())
                .filter(m -> m.getName().equals(name) && m.getParameterCount() == 1)
                .findFirst()
                .orElse(null);
    }

    private void invokeMethod(Method method, Object target, Object arg) {
        try {
            method.invoke(target, arg);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke " + method.getName() + " on " + target.getClass().getSimpleName(), e);
        }
    }

    private Class<?> getListElementType(Type type) {
        if (type instanceof ParameterizedType pt) {
            Type arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class<?> c) return c;
        }
        return String.class;
    }

    @SuppressWarnings("unchecked")
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == String.class || targetType == Object.class) return value;
        if (targetType == Boolean.class) return Boolean.valueOf(value);
        if (targetType == boolean.class) return Boolean.parseBoolean(value);
        if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(value);
        if (List.class.isAssignableFrom(targetType)) {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase().replace(" ", "_"));
        }
        return value;
    }

    private Boolean parseBoolean(String value) {
        return value == null ? null : Boolean.valueOf(value);
    }

    private CreateTableChange.ForeignKeyAction parseForeignKeyAction(String value) {
        if (value == null) return null;
        return CreateTableChange.ForeignKeyAction.valueOf(value.toUpperCase().replace(" ", "_"));
    }
}
