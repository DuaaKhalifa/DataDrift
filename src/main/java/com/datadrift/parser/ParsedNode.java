package com.datadrift.parser;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ParsedNode {

    // The tag/key name (e.g. "createTable", "changeSet", "column")
    private String name;

    // Attributes on this element (e.g. tableName="users")
    private Map<String, String> attributes = new LinkedHashMap<>();

    // Child elements
    private List<ParsedNode> children = new ArrayList<>();

    // Text content of the element (used by <sql>, <where>, etc.)
    private String value;
}
