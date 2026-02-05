package com.datadrift.parser;

import com.datadrift.model.changelog.ChangeSet;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Interface for parsing changelog files into ChangeSet objects.
 * Implementations handle format-specific parsing (XML, YAML, etc.)
 * and convert to the format-agnostic ParsedNode representation.
 */
public interface ChangelogParser {

    List<ChangeSet> parse(File file);

    /** Root element containing all changesets */
    String ELEMENT_DATABASE_CHANGELOG = "databaseChangeLog";

    /** A single migration unit */
    String ELEMENT_CHANGESET = "changeSet";

    /** Comment/description for a changeset */
    String ELEMENT_COMMENT = "comment";

    /** Rollback operations for a changeset */
    String ELEMENT_ROLLBACK = "rollback";

    /** YAML wrapper for change operations (not used in XML) */
    String ELEMENT_CHANGES = "changes";


    /**
     * ChangeSet-level attributes that should be stored as attributes on the ParsedNode.
     */
    Set<String> CHANGESET_ATTRIBUTES = Set.of(
            "id", "author", "context", "labels", "runAlways", "runOnChange", "failOnError"
    );

    /**
     * Change types that have text content as their primary value rather than attributes.
     * For these elements, the text content becomes ParsedNode.value.
     */
    Set<String> TEXT_CONTENT_ELEMENTS = Set.of(
            "sql", "where"
    );
}
