package com.datadrift.service;

import com.datadrift.exception.ChangeSetExecutionException;
import com.datadrift.executor.change.ChangeExecutor;
import com.datadrift.model.change.Change;
import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.model.changelog.DatabaseChangeLog;
import com.datadrift.repository.ChangelogRepository;
import com.datadrift.util.ChecksumUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for executing database changesets.
 * Coordinates with ChangeExecutor implementations to generate and run SQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangelogExecutorService {

    private static final String DATADRIFT_VERSION = "1.0.0";

    private final ChangelogRepository changelogRepository;
    private final Map<String, ChangeExecutor> executorsMap; // Map of change type -> executor

    @Transactional
    public int executePendingChangeSets(List<ChangeSet> changeSets) {
        List<ChangeSet> pending = filterPendingChangeSets(changeSets);

        if (pending.isEmpty()) {
            log.info("No pending changesets to execute");
            return 0;
        }

        log.info("Found {} pending changeset(s) to execute", pending.size());

        int orderExecuted = changelogRepository.getMaxOrderExecuted();
        String deploymentId = UUID.randomUUID().toString().substring(0, 10);

        for (ChangeSet changeSet : pending) {
            orderExecuted++;
            executeChangeSet(changeSet, orderExecuted, deploymentId);
        }

        log.info("Successfully executed {} changeset(s)", pending.size());
        return pending.size();
    }

    public List<ChangeSet> filterPendingChangeSets(List<ChangeSet> changeSets) {
        List<ChangeSet> pending = new ArrayList<>();

        for (ChangeSet changeSet : changeSets) {
            DatabaseChangeLog executed = changelogRepository.findByIdAndAuthor(
                    changeSet.getId(), changeSet.getAuthor()
            );

            if (executed == null) {
                // Never executed - add to pending
                pending.add(changeSet);
            } else if (changeSet.isRunAlways()) {
                // runAlways=true - always execute
                log.debug("ChangeSet {}::{} has runAlways=true, will re-execute",
                        changeSet.getId(), changeSet.getAuthor());
                pending.add(changeSet);
            } else if (changeSet.isRunOnChange()) {
                // runOnChange=true - execute if checksum changed
                String currentChecksum = ChecksumUtil.calculateChecksum(changeSet);
                if (!currentChecksum.equals(executed.getMd5sum())) {
                    log.debug("ChangeSet {}::{} has runOnChange=true and checksum changed, will re-execute",
                            changeSet.getId(), changeSet.getAuthor());
                    pending.add(changeSet);
                }
            }
            // Otherwise: already executed and no special flags, skip
        }

        return pending;
    }

    @Transactional
    public void executeChangeSet(ChangeSet changeSet, int orderExecuted, String deploymentId) {
        log.info("Executing changeset {}::{} from {}",
                changeSet.getId(), changeSet.getAuthor(), changeSet.getFilename());

        try {
            // Execute each change in the changeset
            for (Change change : changeSet.getChanges()) {
                executeChange(change);
            }

            // Record successful execution
            recordExecution(changeSet, orderExecuted, "EXECUTED", deploymentId);

            log.info("Successfully executed changeset {}::{}", changeSet.getId(), changeSet.getAuthor());

        } catch (Exception e) {
            log.error("Failed to execute changeset {}::{}: {}",
                    changeSet.getId(), changeSet.getAuthor(), e.getMessage());

            // Record failed execution if failOnError is true (default)
            if (changeSet.isFailOnError()) {
                recordExecution(changeSet, orderExecuted, "FAILED", deploymentId);
                throw new ChangeSetExecutionException(
                        "Failed to execute changeset " + changeSet.getId() + "::" + changeSet.getAuthor(),
                        e
                );
            } else {
                // Record as MARK_RAN if failOnError is false
                log.warn("Changeset {}::{} failed but failOnError=false, marking as ran",
                        changeSet.getId(), changeSet.getAuthor());
                recordExecution(changeSet, orderExecuted, "MARK_RAN", deploymentId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void executeChange(Change change) {
        String changeType = change.getChangeType();
        ChangeExecutor executor = executorsMap.get(changeType);

        if (executor == null) {
            throw new IllegalStateException("No executor found for change type: " + changeType);
        }

        log.debug("Executing {} change", changeType);
        executor.execute(change);
    }

    @SuppressWarnings("unchecked")
    public String generateSql(ChangeSet changeSet) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- ChangeSet: ").append(changeSet.getId())
                .append("::").append(changeSet.getAuthor()).append("\n");

        for (Change change : changeSet.getChanges()) {
            String changeType = change.getChangeType();
            ChangeExecutor executor = executorsMap.get(changeType);

            if (executor == null) {
                sql.append("-- ERROR: No executor for ").append(changeType).append("\n");
                continue;
            }

            sql.append(executor.generateSql(change)).append(";\n");
        }

        return sql.toString();
    }

    private void recordExecution(ChangeSet changeSet, int orderExecuted, String execType, String deploymentId) {
        DatabaseChangeLog record = new DatabaseChangeLog();
        record.setId(changeSet.getId());
        record.setAuthor(changeSet.getAuthor());
        record.setFilename(changeSet.getFilename());
        record.setDateExecuted(LocalDateTime.now());
        record.setOrderExecuted(orderExecuted);
        record.setExecType(execType);
        record.setMd5sum(ChecksumUtil.calculateChecksum(changeSet));
        record.setDescription(buildDescription(changeSet));
        record.setComments(changeSet.getComment());
        record.setTag(changeSet.getTag());
        record.setVersion(DATADRIFT_VERSION);
        record.setContexts(changeSet.getContext());
        record.setLabels(changeSet.getLabels());
        record.setDeploymentId(deploymentId);

        changelogRepository.save(record);
    }

    private String buildDescription(ChangeSet changeSet) {
        if (changeSet.getChanges() == null || changeSet.getChanges().isEmpty()) {
            return "empty";
        }

        List<String> types = changeSet.getChanges().stream()
                .map(Change::getChangeType)
                .toList();

        String description = String.join(", ", types);
        return description.length() > 250 ? description.substring(0, 250) : description;
    }
}
