package com.datadrift.cli;

import com.datadrift.service.RollbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * CLI command to rollback migrations.
 * Usage: datadrift rollback --count=N
 *        datadrift rollback --tag=tagname
 */
@Slf4j
@Component
@Command(
        name = "rollback",
        description = "Rollback database migrations",
        mixinStandardHelpOptions = true
)
@RequiredArgsConstructor
public class RollbackCommand implements Callable<Integer> {

    private final RollbackService rollbackService;

    @Option(names = {"--count"}, description = "Number of changesets to rollback")
    private Integer count;

    @Option(names = {"--tag"}, description = "Rollback to a specific tag")
    private String tag;

    @Override
    public Integer call() {
        log.info("Starting rollback...");

        // Validate options: must provide exactly one of --count or --tag
        if (count == null && tag == null) {
            System.err.println("Error: You must specify either --count or --tag");
            System.err.println();
            System.err.println("Usage:");
            System.err.println("  datadrift rollback --count=N    Rollback N changesets");
            System.err.println("  datadrift rollback --tag=TAG    Rollback to a specific tag");
            return 1;
        }

        if (count != null && tag != null) {
            System.err.println("Error: Cannot specify both --count and --tag");
            return 1;
        }

        try {
            int rolledBack;

            if (count != null) {
                log.info("Rolling back {} changeset(s)...", count);
                System.out.println("DataDrift - Rolling Back " + count + " Changeset(s)");
                System.out.println("==========================================");
                System.out.println();

                rolledBack = rollbackService.rollback(count);
            } else {
                log.info("Rolling back to tag '{}'...", tag);
                System.out.println("DataDrift - Rolling Back to Tag '" + tag + "'");
                System.out.println("==========================================");
                System.out.println();

                rolledBack = rollbackService.rollbackToTag(tag);
            }

            if (rolledBack == 0) {
                log.info("No changesets were rolled back");
                System.out.println("No changesets were rolled back.");
            } else {
                log.info("Successfully rolled back {} changeset(s)", rolledBack);
                System.out.println("Successfully rolled back " + rolledBack + " changeset(s).");
            }

            System.out.println();
            System.out.println("Rollback completed successfully.");
            return 0;

        } catch (Exception e) {
            log.error("Rollback failed: {}", e.getMessage(), e);
            System.err.println();
            System.err.println("Rollback FAILED: " + e.getMessage());
            return 1;
        }
    }
}
