package com.datadrift.service;

import com.datadrift.exception.ChecksumMismatchException;
import com.datadrift.exception.ValidationException;
import com.datadrift.model.change.Change;
import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.model.changelog.DatabaseChangeLog;
import com.datadrift.repository.ChangelogRepository;
import com.datadrift.util.ChecksumUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for validating changelog files and changesets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ChangelogRepository changelogRepository;

    public void validate(List<ChangeSet> changeSets) {
        List<String> errors = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (int i = 0; i < changeSets.size(); i++) {
            ChangeSet changeSet = changeSets.get(i);
            String location = "ChangeSet #" + (i + 1);

            // Check required fields
            if (changeSet.getId() == null || changeSet.getId().isBlank()) {
                errors.add(location + ": Missing required field 'id'");
            }

            if (changeSet.getAuthor() == null || changeSet.getAuthor().isBlank()) {
                errors.add(location + ": Missing required field 'author'");
            }

            if (changeSet.getChanges() == null || changeSet.getChanges().isEmpty()) {
                errors.add(location + " (" + changeSet.getId() + "): No changes defined");
            }

            // Check for duplicates (id + author combination must be unique)
            if (changeSet.getId() != null && changeSet.getAuthor() != null) {
                String uniqueKey = changeSet.getId() + "::" + changeSet.getAuthor();
                if (seenIds.contains(uniqueKey)) {
                    errors.add(location + ": Duplicate changeset id '" + changeSet.getId() +
                            "' for author '" + changeSet.getAuthor() + "'");
                }
                seenIds.add(uniqueKey);
            }

            // Validate each change within the changeset
            if (changeSet.getChanges() != null) {
                for (int j = 0; j < changeSet.getChanges().size(); j++) {
                    Change change = changeSet.getChanges().get(j);
                    try {
                        change.validate();
                    } catch (Exception e) {
                        errors.add(location + " (" + changeSet.getId() + "), Change #" + (j + 1) +
                                " (" + change.getChangeType() + "): " + e.getMessage());
                    }
                }
            }

            // Validate rollback changes if present
            if (changeSet.getRollbackChanges() != null) {
                for (int j = 0; j < changeSet.getRollbackChanges().size(); j++) {
                    Change change = changeSet.getRollbackChanges().get(j);
                    try {
                        change.validate();
                    } catch (Exception e) {
                        errors.add(location + " (" + changeSet.getId() + "), Rollback Change #" + (j + 1) +
                                " (" + change.getChangeType() + "): " + e.getMessage());
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            String errorMessage = "Validation failed with " + errors.size() + " error(s):\n  - " +
                    String.join("\n  - ", errors);
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        log.info("Validated {} changesets successfully", changeSets.size());
    }

    public void validateChecksums(List<ChangeSet> changeSets) {
        List<String> mismatches = new ArrayList<>();

        for (ChangeSet changeSet : changeSets) {
            DatabaseChangeLog executed = changelogRepository.findByIdAndAuthor(
                    changeSet.getId(), changeSet.getAuthor()
            );

            // Skip if not yet executed
            if (executed == null) {
                continue;
            }

            // Calculate current checksum
            String currentChecksum = ChecksumUtil.calculateChecksum(changeSet);
            String storedChecksum = executed.getMd5sum();

            // Compare checksums
            if (storedChecksum != null && !storedChecksum.equals(currentChecksum)) {
                mismatches.add(String.format(
                        "ChangeSet '%s::%s' (file: %s) was modified after execution. " +
                                "Stored checksum: %s, Current checksum: %s",
                        changeSet.getId(), changeSet.getAuthor(), changeSet.getFilename(),
                        storedChecksum, currentChecksum
                ));
            }
        }

        if (!mismatches.isEmpty()) {
            String errorMessage = "Checksum validation failed:\n  - " +
                    String.join("\n  - ", mismatches);
            log.error(errorMessage);
            throw new ChecksumMismatchException(errorMessage);
        }

        log.debug("Checksum validation passed for {} changesets", changeSets.size());
    }
}
