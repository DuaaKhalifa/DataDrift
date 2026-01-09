package com.datadrift.cli;

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
     * Execute the status command.
     *
     * Should:
     * 1. Call migrationService.getStatus()
     * 2. Print formatted output showing:
     *    - Total changesets
     *    - Executed changesets
     *    - Pending changesets
     *    - List of pending changeset IDs
     * 3. Return 0
     */
    @Override
    public Integer call() {
        // TODO: Implement status command
        return 0;
    }
}
