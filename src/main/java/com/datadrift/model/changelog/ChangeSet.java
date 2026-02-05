package com.datadrift.model.changelog;

import com.datadrift.model.change.Change;
import lombok.Data;

import java.util.List;

/**
 * Represents a single changeset in a migration file.
 * A changeset is the atomic unit of database change.
 */
@Data
public class ChangeSet {
    private String id;
    private String author;
    private String comment;
    private List<Change> changes;
    private List<Change> rollbackChanges;
    private String context;
    private String labels;
    private boolean runAlways;
    private boolean runOnChange;
    private boolean failOnError = true;
    private String filename;
}
