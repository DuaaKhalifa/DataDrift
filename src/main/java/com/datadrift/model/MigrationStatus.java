package com.datadrift.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents the current status of database migrations.
 */
@Data
public class MigrationStatus {
    private int totalChangesets;
    private int executedCount;
    private int pendingCount;
    private List<String> pendingChangesets;
    private LocalDateTime lastExecutionTime;
    private boolean locked;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Migration Status:\n");
        sb.append("  Total changesets:    ").append(totalChangesets).append("\n");
        sb.append("  Executed:            ").append(executedCount).append("\n");
        sb.append("  Pending:             ").append(pendingCount).append("\n");
        sb.append("  Last execution:      ").append(lastExecutionTime != null ? lastExecutionTime : "Never").append("\n");
        sb.append("  Lock status:         ").append(locked ? "LOCKED" : "Not locked").append("\n");

        if (pendingChangesets != null && !pendingChangesets.isEmpty()) {
            sb.append("  Pending changesets:\n");
            for (String id : pendingChangesets) {
                sb.append("    - ").append(id).append("\n");
            }
        }

        return sb.toString();
    }
}
