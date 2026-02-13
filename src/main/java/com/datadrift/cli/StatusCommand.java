package com.datadrift.cli;

import com.datadrift.model.MigrationStatus;
import com.datadrift.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * CLI command to check migration status.
 * Usage: datadrift status
 */
@Slf4j
@Component
@Command(
        name = "status",
        description = "Show migration status (executed and pending changesets)",
        mixinStandardHelpOptions = true
)
@RequiredArgsConstructor
public class StatusCommand implements Callable<Integer> {

    private final MigrationService migrationService;

    /**
     * Sample:
     *   Migration Status:
     *   Total changesets:    5
     *   Executed:            3
     *   Pending:             2
     *   Last execution:      2024-01-15T10:30:00
     *   Lock status:         Not locked
     *   Pending changesets:
     *     - changeset-004
     *     - changeset-005
     */
    @Override
    public Integer call() {
        log.info("Checking migration status...");

        try {
            MigrationStatus status = migrationService.getStatus();

            System.out.println("DataDrift - Migration Status");
            System.out.println("============================");
            System.out.println();
            System.out.println(status.toString());

            if (status.isLocked()) {
                log.warn("Database is currently locked");
                System.out.println("WARNING: Database is locked. Another migration may be in progress.");
            }

            log.info("Status check completed: {} total, {} executed, {} pending",
                    status.getTotalChangesets(), status.getExecutedCount(), status.getPendingCount());
            return 0;

        } catch (Exception e) {
            log.error("Failed to retrieve migration status: {}", e.getMessage(), e);
            System.err.println("Failed to retrieve migration status: " + e.getMessage());
            return 1;
        }
    }
}
