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

    /**
     * Execute the rollback command.
     *
     * Should:
     * 1. Validate that either count or tag is provided (not both)
     * 2. If count is provided, call rollbackService.rollback(count)
     * 3. If tag is provided, call rollbackService.rollbackToTag(tag)
     * 4. Handle exceptions and print error messages
     * 5. Print success message
     * 6. Return 0 for success, 1 for failure
     */
    @Override
    public Integer call() {
        // TODO: Implement rollback command
        return 0;
    }
}
