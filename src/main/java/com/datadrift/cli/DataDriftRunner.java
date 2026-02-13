package com.datadrift.cli;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Spring Boot runner that integrates Picocli with Spring.
 * Executes CLI commands and propagates exit codes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataDriftRunner implements CommandLineRunner, ExitCodeGenerator {

    private final IFactory factory;
    private int exitCode;

    @Override
    public void run(String... args) throws Exception {
        log.debug("DataDrift CLI starting with args: {}", (Object) args);

        exitCode = new CommandLine(DataDriftCli.class, factory).execute(args);

        log.debug("DataDrift CLI completed with exit code: {}", exitCode);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
