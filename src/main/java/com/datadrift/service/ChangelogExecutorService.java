package com.datadrift.service;

import com.datadrift.executor.change.ChangeExecutor;
import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.repository.ChangelogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service for executing database changesets.
 * Coordinates with ChangeExecutor implementations to generate and run SQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangelogExecutorService {

    private final ChangelogRepository changelogRepository;
    private final Map<String, ChangeExecutor> executors; // Map of change type -> executor

    /**
     * Execute all pending changesets.
     *
     * Should:
     * 1. Query DATABASECHANGELOG table to get list of already-executed changesets
     * 2. Filter changeSets list to only include pending (not yet executed) changesets
     * 3. For each pending changeset, call executeChangeSet()
     * 4. Maintain order of execution (orderExecuted counter)
     */
    @Transactional
    public void executePendingChangeSets(List<ChangeSet> changeSets) {
        // TODO: Implement execution of pending changesets
    }

    /**
     * Execute a single changeset.
     *
     * Should:
     * 1. Iterate through each Change in the changeset
     * 2. Get the appropriate ChangeExecutor based on change type
     * 3. Call executor.execute(change) to run the SQL
     * 4. If successful, record execution in DATABASECHANGELOG table with EXECUTED status
     * 5. If failed, record with FAILED status and throw exception
     * 6. Calculate MD5 checksum of changeset for integrity checking
     */
    @Transactional
    public void executeChangeSet(ChangeSet changeSet, int orderExecuted) {
        // TODO: Implement single changeset execution
    }

    /**
     * Record a changeset execution in DATABASECHANGELOG table.
     *
     * Should:
     * 1. Create DatabaseChangeLog record with all metadata
     * 2. Include: id, author, filename, dateExecuted, orderExecuted, execType, md5sum
     * 3. Save to database using changelogRepository
     */
    private void recordExecution(ChangeSet changeSet, int orderExecuted, String execType) {
        // TODO: Implement execution recording
    }
}
