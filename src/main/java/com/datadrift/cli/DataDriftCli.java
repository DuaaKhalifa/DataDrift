package com.datadrift.cli;

import picocli.CommandLine.Command;

/**
 * Parent CLI command that groups all DataDrift subcommands.
 * Usage: datadrift <command> [options]
 */
@Command(
        name = "datadrift",
        description = "Database migration management tool",
        subcommands = {
                MigrateCommand.class,
                StatusCommand.class,
                RollbackCommand.class,
                ValidateCommand.class,
                GenerateSqlCommand.class
        },
        mixinStandardHelpOptions = true,
        version = "DataDrift 1.0.0"
)
public class DataDriftCli implements Runnable {

    @Override
    public void run() {
        // When no subcommand is provided, print usage help
        System.out.println("DataDrift - Database Migration Management Tool");
        System.out.println();
        System.out.println("Usage: datadrift <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  migrate       Execute all pending database migrations");
        System.out.println("  status        Show migration status (executed and pending)");
        System.out.println("  rollback      Rollback database migrations");
        System.out.println("  validate      Validate migration files without executing");
        System.out.println("  generate-sql  Generate SQL for pending migrations");
        System.out.println();
        System.out.println("Run 'datadrift <command> --help' for more information on a command.");
    }
}
