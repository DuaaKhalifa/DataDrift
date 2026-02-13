package com.datadrift.cli;

import com.datadrift.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * CLI command to generate SQL preview without executing.
 * Usage: datadrift generate-sql [--output=file.sql]
 */
@Slf4j
@Component
@Command(
        name = "generate-sql",
        description = "Generate SQL for pending migrations without executing",
        mixinStandardHelpOptions = true
)
@RequiredArgsConstructor
public class GenerateSqlCommand implements Callable<Integer> {

    private final MigrationService migrationService;

    @Option(names = {"--output"}, description = "Output file path (prints to console if not specified)")
    private String outputFile;

    @Override
    public Integer call() {
        log.info("Generating SQL for pending migrations...");

        try {
            String sql = migrationService.generateSql();

            if (sql == null || sql.isBlank()) {
                log.info("No pending migrations found");
                System.out.println("No pending migrations to generate SQL for.");
                return 0;
            }

            if (outputFile != null) {
                log.info("Writing SQL to file: {}", outputFile);
                Files.writeString(Path.of(outputFile), sql);
                System.out.println("SQL written to: " + outputFile);
            } else {
                System.out.println("-- Generated SQL for pending migrations");
                System.out.println("-- =====================================");
                System.out.println();
                System.out.println(sql);
            }

            log.info("SQL generation completed successfully");
            return 0;

        } catch (IOException e) {
            log.error("Failed to write SQL to file: {}", e.getMessage(), e);
            System.err.println("Failed to write SQL to file: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            log.error("SQL generation failed: {}", e.getMessage(), e);
            System.err.println("SQL generation FAILED: " + e.getMessage());
            return 1;
        }
    }
}
