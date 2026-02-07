package com.datadrift.service;

import com.datadrift.model.MigrationStatus;
import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.model.changelog.DatabaseChangeLog;
import com.datadrift.repository.ChangelogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Main service for orchestrating database migrations.
 * Coordinates parsing, validation, and execution of changesets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationService {

    private final ChangelogParserService parserService;
    private final ChangelogExecutorService executorService;
    private final ValidationService validationService;
    private final LockService lockService;
    private final ChangelogRepository changelogRepository;

    public int migrate() {
        log.info("Starting migration...");

        lockService.acquireLock();
        try {
            // Parse all changelog files
            List<ChangeSet> allChangeSets = parserService.parseAllChangelogs();
            log.info("Parsed {} changeset(s) from changelog files", allChangeSets.size());

            if (allChangeSets.isEmpty()) {
                log.info("No changesets found in changelog files");
                return 0;
            }

            // Validate changesets
            validationService.validate(allChangeSets);

            // Validate checksums of already-executed changesets
            validationService.validateChecksums(allChangeSets);

            // Execute pending changesets
            int executed = executorService.executePendingChangeSets(allChangeSets);

            log.info("Migration completed. {} changeset(s) executed.", executed);
            return executed;

        } finally {
            lockService.releaseLock();
        }
    }

    public MigrationStatus getStatus() {
        // Parse all changesets
        List<ChangeSet> allChangeSets = parserService.parseAllChangelogs();

        // Get executed changesets from database
        List<DatabaseChangeLog> executed = changelogRepository.findAll();

        // Filter to find pending changesets
        List<ChangeSet> pending = executorService.filterPendingChangeSets(allChangeSets);

        // Find last execution time
        LocalDateTime lastExecuted = executed.stream()
                .map(DatabaseChangeLog::getDateExecuted)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // Build pending changeset identifiers
        List<String> pendingIds = pending.stream()
                .map(cs -> cs.getId() + "::" + cs.getAuthor())
                .toList();

        // Check lock status
        boolean isLocked = lockService.isLocked();

        MigrationStatus status = new MigrationStatus();
        status.setTotalChangesets(allChangeSets.size());
        status.setExecutedCount(executed.size());
        status.setPendingCount(pending.size());
        status.setPendingChangesets(pendingIds);
        status.setLastExecutionTime(lastExecuted);
        status.setLocked(isLocked);

        return status;
    }

    public String generateSql() {
        // Parse all changesets
        List<ChangeSet> allChangeSets = parserService.parseAllChangelogs();

        // Filter to find pending changesets
        List<ChangeSet> pending = executorService.filterPendingChangeSets(allChangeSets);

        if (pending.isEmpty()) {
            return "-- No pending changesets";
        }

        StringBuilder sql = new StringBuilder();
        sql.append("-- DataDrift Migration SQL Preview\n");
        sql.append("-- Generated at: ").append(LocalDateTime.now()).append("\n");
        sql.append("-- Pending changesets: ").append(pending.size()).append("\n\n");

        for (ChangeSet changeSet : pending) {
            sql.append(executorService.generateSql(changeSet));
            sql.append("\n");
        }

        return sql.toString();
    }
}
