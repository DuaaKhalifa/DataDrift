package com.datadrift.model.changelog;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a record in the DATABASECHANGELOG table.
 * Tracks executed changesets.
 */
@Data
public class DatabaseChangeLog {
    private String id;
    private String author;
    private String filename;
    private LocalDateTime dateExecuted;
    private Integer orderExecuted;
    private String execType;
    private String md5sum;
    private String description;
    private String comments;
    private String tag;
    private String version;
    private String contexts;
    private String labels;
    private String deploymentId;
}
