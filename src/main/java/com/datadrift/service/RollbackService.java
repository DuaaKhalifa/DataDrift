package com.datadrift.service;

import com.datadrift.exception.RollbackException;
import com.datadrift.executor.change.ChangeExecutor;
import com.datadrift.model.change.Change;
import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.model.changelog.DatabaseChangeLog;
import com.datadrift.repository.ChangelogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service for rolling back database migrations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RollbackService {

    private final ChangelogRepository changelogRepository;
    private final ChangelogParserService parserService;
    private final Map<String, ChangeExecutor> executors;

    /**
     * Rollback the last N changesets.
     */
    @Transactional
    public int rollback(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Rollback count must be positive");
        }

        List<DatabaseChangeLog> lastExecuted = changelogRepository.findLastN(count);

        if (lastExecuted.isEmpty()) {
            log.info("No changesets to rollback");
            return 0;
        }

        log.info("Rolling back {} changeset(s)", lastExecuted.size());

        // Get all parsed changesets to find rollback definitions
        List<ChangeSet> allChangeSets = parserService.parseAllChangelogs();

        int rolledBack = 0;
        for (DatabaseChangeLog executed : lastExecuted) {
            ChangeSet changeSet = findChangeSet(allChangeSets, executed.getId(), executed.getAuthor());

            if (changeSet == null) {
                throw new RollbackException(
                        "Cannot find changeset definition for " + executed.getId() + "::" + executed.getAuthor() +
                                ". Ensure the changelog file still exists."
                );
            }

            rollbackChangeSet(changeSet);
            rolledBack++;
        }

        log.info("Successfully rolled back {} changeset(s)", rolledBack);
        return rolledBack;
    }

    /**
     * Rollback to a specific tag.
     * Rolls back all changesets executed after the tagged changeset.
     *
     * @param tag The tag to rollback to
     * @return Number of changesets rolled back
     */
    @Transactional
    public int rollbackToTag(String tag) {
        List<DatabaseChangeLog> tagged = changelogRepository.findByTag(tag);

        if (tagged.isEmpty()) {
            throw new RollbackException("No changeset found with tag: " + tag);
        }

        // Get the lowest orderExecuted among tagged changesets
        int tagOrder = tagged.stream()
                .mapToInt(DatabaseChangeLog::getOrderExecuted)
                .min()
                .orElse(0);

        // Get all changesets executed after the tag
        List<DatabaseChangeLog> toRollback = changelogRepository.findAfterOrder(tagOrder);

        if (toRollback.isEmpty()) {
            log.info("No changesets to rollback after tag '{}'", tag);
            return 0;
        }

        log.info("Rolling back {} changeset(s) to tag '{}'", toRollback.size(), tag);

        // Get all parsed changesets to find rollback definitions
        List<ChangeSet> allChangeSets = parserService.parseAllChangelogs();

        int rolledBack = 0;
        for (DatabaseChangeLog executed : toRollback) {
            ChangeSet changeSet = findChangeSet(allChangeSets, executed.getId(), executed.getAuthor());

            if (changeSet == null) {
                throw new RollbackException(
                        "Cannot find changeset definition for " + executed.getId() + "::" + executed.getAuthor()
                );
            }

            rollbackChangeSet(changeSet);
            rolledBack++;
        }

        log.info("Successfully rolled back {} changeset(s) to tag '{}'", rolledBack, tag);
        return rolledBack;
    }

    @Transactional
    public void rollbackChangeSet(ChangeSet changeSet) {
        log.info("Rolling back changeset {}::{}", changeSet.getId(), changeSet.getAuthor());

        List<Change> rollbackChanges = changeSet.getRollbackChanges();

        if (rollbackChanges == null || rollbackChanges.isEmpty()) {
            throw new RollbackException(
                    "No rollback defined for changeset " + changeSet.getId() + "::" + changeSet.getAuthor()
            );
        }

        // Execute each rollback change
        for (Change change : rollbackChanges) {
            executeRollbackChange(change);
        }

        // Remove the record from DATABASECHANGELOG
        int deleted = changelogRepository.delete(changeSet.getId(), changeSet.getAuthor());

        if (deleted > 0) {
            log.info("Successfully rolled back changeset {}::{}", changeSet.getId(), changeSet.getAuthor());
        } else {
            log.warn("Changeset {}::{} was not in DATABASECHANGELOG table",
                    changeSet.getId(), changeSet.getAuthor());
        }
    }

    @SuppressWarnings("unchecked")
    private void executeRollbackChange(Change change) {
        String changeType = change.getChangeType();
        ChangeExecutor executor = executors.get(changeType);

        if (executor == null) {
            throw new IllegalStateException("No executor found for rollback change type: " + changeType);
        }

        log.debug("Executing rollback {} change", changeType);
        executor.execute(change);
    }

    /**
     * Find a changeset by ID and author from the list of parsed changesets.
     */
    private ChangeSet findChangeSet(List<ChangeSet> changeSets, String id, String author) {
        return changeSets.stream()
                .filter(cs -> id.equals(cs.getId()) && author.equals(cs.getAuthor()))
                .findFirst()
                .orElse(null);
    }
}
