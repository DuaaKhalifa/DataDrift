package com.datadrift.cli;

import com.datadrift.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * CLI command to execute pending migrations.
 * Usage: datadrift migrate
 */
@Slf4j
@Component
@Command(
        name = "migrate",
        description = "Execute all pending database migrations",
        mixinStandardHelpOptions = true
)
@RequiredArgsConstructor
public class MigrateCommand implements Callable<Integer> {

    private final MigrationService migrationService;

    @Override
    public Integer call() {
        log.info("Starting database migration...");
        System.out.println("DataDrift - Executing pending migrations");
        System.out.println();

        try {
            int executedCount = migrationService.migrate();

            if (executedCount == 0) {
                log.info("No pending migrations found");
                System.out.println("No pending migrations to execute.");
            } else {
                log.info("Successfully executed {} changeset(s)", executedCount);
                System.out.println("Successfully executed " + executedCount + " changeset(s).");
            }
            System.out.println();
            System.out.println("Migration completed successfully.");
            return 0;

        } catch (Exception e) {
            log.error("Migration failed: {}", e.getMessage(), e);
            System.err.println();
            System.err.println("Migration FAILED: " + e.getMessage());
            return 1;
        }
    }
}
