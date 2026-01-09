# DataDrift

A modern database change management tool built with Java and Spring Boot. DataDrift provides version control for database schemas, migration management, and safe rollback mechanisms.

## Tech Stack

- **Java**: 21 (LTS)
- **Build Tool**: Maven 3.9+
- **Framework**: Spring Boot 3.2+ (JDBC only)
- **Database**: PostgreSQL with JDBC and HikariCP connection pool
- **CLI**: Picocli 4.7+
- **Logging**: SLF4J with Logback
- **Testing**: JUnit 5, Mockito, Testcontainers (PostgreSQL), JMH (benchmarking)
- **XML Parsing**: JAXB and Jackson XML
- **CI/CD**: GitHub Actions

## Prerequisites

- Java 21 or higher
- Maven 3.9+
- PostgreSQL 16+ (or Docker to run PostgreSQL)

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/DataDrift.git
cd DataDrift
```

### 2. Build the project

```bash
mvn clean install
```

### 3. Run the CLI

Ensure PostgreSQL is running locally, then:

```bash
java -jar target/datadrift-1.0.0-SNAPSHOT.jar --help
```

Or during development:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--help"
```

## Configuration

Application configuration is in `src/main/resources/application.yml`.

Key configurations:
- Database connection settings
- HikariCP pool configuration
- Logging levels

### Profiles

- `dev` - Development profile (see `application-dev.yml`)

To run with a specific profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Migration Files

DataDrift supports two formats for defining database migrations: **XML** and **YAML**.

### File Location

Place your migration files in:
```
src/main/resources/db/changelog/
```

### Naming Convention

Use a descriptive naming pattern for your migration files:
```
<sequence>-<description>.<xml|yaml>

Examples:
- 001-create-users-table.xml
- 002-add-user-roles.yaml
- 003-create-products-schema.xml
```

### Supported Change Types

DataDrift supports the following change operations:
- `createTable` / `dropTable`
- `addColumn` / `dropColumn`
- `createIndex` / `dropIndex`
- `addForeignKeyConstraint` / `dropForeignKeyConstraint`
- `insert` / `update` / `delete`
- `sql` - for custom SQL statements
- And more...

### XML Format

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.datadrift.com/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <changeSet id="001" author="your.name">
        <comment>Description of the change</comment>

        <createTable tableName="users">
            <column name="id" type="BIGSERIAL">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="username" type="VARCHAR(100)">
                <constraints nullable="false" unique="true"/>
            </column>
        </createTable>

        <rollback>
            <dropTable tableName="users"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

See `src/main/resources/db/changelog/example-001-create-users-table.xml` for a complete example.

### YAML Format

```yaml
databaseChangeLog:
  - changeSet:
      id: 001
      author: your.name
      comment: Description of the change
      changes:
        - createTable:
            tableName: users
            columns:
              - column:
                  name: id
                  type: BIGSERIAL
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: username
                  type: VARCHAR(100)
                  constraints:
                    nullable: false
                    unique: true
      rollback:
        - dropTable:
            tableName: users
```

See `src/main/resources/db/changelog/example-002-create-products-table.yaml` for a complete example.

### Rollback Support

Each changeset can optionally define rollback operations. This allows you to revert changes if needed:

```xml
<changeSet id="001" author="your.name">
    <createTable tableName="users">
        <!-- columns -->
    </createTable>

    <rollback>
        <dropTable tableName="users"/>
    </rollback>
</changeSet>
```

### Change Tracking

DataDrift automatically tracks executed migrations using two internal tables:
- `DATABASECHANGELOG` - Records all executed changesets
- `DATABASECHANGELOGLOCK` - Prevents concurrent migrations

These tables are created automatically on first run. See `src/main/resources/db/schema/tracking-tables.sql` for the schema definition.

## User Workflow

### 1. Create a Migration

Create a new XML or YAML file in `src/main/resources/db/changelog/`:

```bash
# Using XML
touch src/main/resources/db/changelog/001-create-users-table.xml

# Using YAML
touch src/main/resources/db/changelog/002-add-products.yaml
```

Define your changeset using the appropriate format (see Migration Files section above).

### 2. Apply Migrations

Run all pending migrations:

```bash
java -jar target/datadrift-1.0.0-SNAPSHOT.jar migrate
```

Or during development:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="migrate"
```

This will:
- Check for any pending migrations
- Execute them in order
- Record successful executions in `DATABASECHANGELOG`
- Prevent concurrent migrations using `DATABASECHANGELOGLOCK`

### 3. Check Migration Status

View which migrations have been applied:

```bash
java -jar target/datadrift-1.0.0-SNAPSHOT.jar status
```

This shows:
- Total changesets in your changelog files
- Which changesets have been executed
- Which are pending

### 4. Rollback Migrations

Rollback the last N migrations:

```bash
# Rollback last migration
java -jar target/datadrift-1.0.0-SNAPSHOT.jar rollback --count=1

# Rollback last 3 migrations
java -jar target/datadrift-1.0.0-SNAPSHOT.jar rollback --count=3
```

Rollback to a specific tag:

```bash
java -jar target/datadrift-1.0.0-SNAPSHOT.jar rollback --tag=v1.0.0
```

### 5. Validate Migrations

Validate your migration files without executing them:

```bash
java -jar target/datadrift-1.0.0-SNAPSHOT.jar validate
```

This checks:
- Migration file syntax
- Checksum integrity (detects modifications to already-executed migrations)
- Structural correctness

### 6. Generate SQL Preview

Preview the SQL that will be executed without applying changes:

```bash
java -jar target/datadrift-1.0.0-SNAPSHOT.jar generate-sql
```

This is useful for:
- Reviewing changes before applying
- Generating SQL for manual execution in production
- Understanding what DataDrift will do

### Common CLI Commands

```bash
# Show all available commands
java -jar target/datadrift-1.0.0-SNAPSHOT.jar --help

# Apply all pending migrations
java -jar target/datadrift-1.0.0-SNAPSHOT.jar migrate

# Check migration status
java -jar target/datadrift-1.0.0-SNAPSHOT.jar status

# Validate migrations
java -jar target/datadrift-1.0.0-SNAPSHOT.jar validate

# Rollback last N changes
java -jar target/datadrift-1.0.0-SNAPSHOT.jar rollback --count=N

# Generate SQL preview
java -jar target/datadrift-1.0.0-SNAPSHOT.jar generate-sql

# Tag current database state
java -jar target/datadrift-1.0.0-SNAPSHOT.jar tag --name=v1.0.0
```

## Running Tests

```bash
mvn test
```

## Project Structure

```
DataDrift/
├── src/
│   ├── main/
│   │   ├── java/com/datadrift/
│   │   │   ├── DataDriftApplication.java    # Spring Boot application entry point
│   │   │   │
│   │   │   ├── cli/                          # CLI Commands (Picocli)
│   │   │   │   ├── MigrateCommand.java       # Execute migrations
│   │   │   │   ├── StatusCommand.java        # Show migration status
│   │   │   │   ├── RollbackCommand.java      # Rollback migrations
│   │   │   │   ├── ValidateCommand.java      # Validate changelog files
│   │   │   │   └── GenerateSqlCommand.java   # Generate SQL preview
│   │   │   │
│   │   │   ├── config/                       # Spring Configuration
│   │   │   │   └── (Spring Boot configs)
│   │   │   │
│   │   │   ├── model/                        # Domain Models
│   │   │   │   ├── changelog/
│   │   │   │   │   ├── ChangeSet.java        # Represents a changeset
│   │   │   │   │   ├── DatabaseChangeLog.java # DATABASECHANGELOG table model
│   │   │   │   │   └── DatabaseChangeLogLock.java # DATABASECHANGELOGLOCK table model
│   │   │   │   └── change/
│   │   │   │       ├── Change.java           # Interface for all change types
│   │   │   │       ├── CreateTableChange.java
│   │   │   │       ├── AddColumnChange.java
│   │   │   │       ├── SqlChange.java
│   │   │   │       └── ... (other change types)
│   │   │   │
│   │   │   ├── parser/                       # Changelog Parsers
│   │   │   │   ├── xml/
│   │   │   │   │   └── XmlChangelogParser.java
│   │   │   │   └── yaml/
│   │   │   │       └── YamlChangelogParser.java
│   │   │   │
│   │   │   ├── executor/                     # SQL Executors
│   │   │   │   └── change/
│   │   │   │       ├── ChangeExecutor.java   # Interface for executors
│   │   │   │       ├── CreateTableExecutor.java
│   │   │   │       ├── AddColumnExecutor.java
│   │   │   │       ├── SqlExecutor.java
│   │   │   │       └── ... (other executors)
│   │   │   │
│   │   │   ├── service/                      # Business Logic
│   │   │   │   ├── MigrationService.java     # Orchestrates migrations
│   │   │   │   ├── ChangelogParserService.java # Parses changelog files
│   │   │   │   ├── ChangelogExecutorService.java # Executes changesets
│   │   │   │   ├── RollbackService.java      # Handles rollbacks
│   │   │   │   ├── ValidationService.java    # Validates changesets
│   │   │   │   └── LockService.java          # Manages migration locks
│   │   │   │
│   │   │   ├── repository/                   # Data Access Layer
│   │   │   │   ├── ChangelogRepository.java  # DATABASECHANGELOG CRUD
│   │   │   │   └── LockRepository.java       # DATABASECHANGELOGLOCK CRUD
│   │   │   │
│   │   │   ├── exception/                    # Custom Exceptions
│   │   │   │   ├── MigrationLockException.java
│   │   │   │   ├── MigrationExecutionException.java
│   │   │   │   └── ChangelogParseException.java
│   │   │   │
│   │   │   └── util/                         # Utilities
│   │   │       └── ChecksumUtil.java         # MD5 checksum calculation
│   │   │
│   │   └── resources/
│   │       ├── application.yml               # Main configuration
│   │       ├── application-dev.yml           # Dev profile configuration
│   │       └── db/
│   │           ├── changelog/                # Migration files (XML/YAML)
│   │           │   ├── example-001-create-users-table.xml
│   │           │   └── example-002-create-products-table.yaml
│   │           └── schema/
│   │               └── tracking-tables.sql   # Tracking tables schema
│   │
│   └── test/
│       ├── java/com/datadrift/              # Unit and integration tests
│       └── resources/                        # Test resources
│
├── .github/
│   └── workflows/
│       └── ci.yml                            # GitHub Actions CI/CD
│
└── pom.xml                                   # Maven configuration
```

### Package Responsibilities

**cli/** - Picocli command implementations. Each command delegates to appropriate service.

**model/changelog/** - Domain models representing changesets and tracking tables.

**model/change/** - Change type implementations (createTable, addColumn, etc.).

**parser/** - Parsers that convert XML/YAML files into ChangeSet objects.

**executor/** - Executors that generate and execute SQL for each change type.

**service/** - Business logic layer that orchestrates parsing, validation, and execution.

**repository/** - Data access layer using JdbcTemplate for tracking tables.

**exception/** - Custom exceptions for migration errors.

**util/** - Utility classes for checksums, etc.

## License

This project is licensed under the terms specified in the LICENSE file.
