package com.datadrift.model.changelog;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a record in the DATABASECHANGELOGLOCK table.
 * Prevents concurrent migration execution.
 */
@Data
public class DatabaseChangeLogLock {
    private Integer id;
    private Boolean locked;
    private LocalDateTime lockGranted;
    private String lockedBy;
}
