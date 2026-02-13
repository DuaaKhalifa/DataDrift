package com.datadrift.cli;

import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.service.ChangelogParserService;
import com.datadrift.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI command to validate migration files.
 * Usage: datadrift validate
 */
@Slf4j
@Component
@Command(
        name = "validate",
        description = "Validate migration files without executing them",
        mixinStandardHelpOptions = true
)
@RequiredArgsConstructor
public class ValidateCommand implements Callable<Integer> {

    private final ChangelogParserService parserService;
    private final ValidationService validationService;

    @Override
    public Integer call() {
        log.info("Validating changelog files...");
        System.out.println("DataDrift - Validating Changelog Files");
        System.out.println("======================================");
        System.out.println();

        try {
            // Step 1: Parse all changelog files
            log.info("Parsing changelog files...");
            System.out.println("Parsing changelog files...");
            List<ChangeSet> changeSets = parserService.parseAllChangelogs();

            if (changeSets.isEmpty()) {
                log.info("No changesets found to validate");
                System.out.println("No changesets found.");
                return 0;
            }

            System.out.println("Found " + changeSets.size() + " changeset(s).");
            System.out.println();

            // Step 2: Validate changesets
            log.info("Validating {} changesets...", changeSets.size());
            System.out.println("Validating changesets...");
            validationService.validate(changeSets);

            // Step 3: Validate checksums against database
            log.info("Validating checksums...");
            System.out.println("Validating checksums against database...");
            validationService.validateChecksums(changeSets);

            // Success
            log.info("Validation completed successfully");
            System.out.println();
            System.out.println("Validation PASSED: All " + changeSets.size() + " changeset(s) are valid.");
            return 0;

        } catch (Exception e) {
            log.error("Validation failed: {}", e.getMessage(), e);
            System.err.println();
            System.err.println("Validation FAILED: " + e.getMessage());
            return 1;
        }
    }
}
