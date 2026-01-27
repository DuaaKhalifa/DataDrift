package com.datadrift.executor.change;

import com.datadrift.model.change.CreateTableChange;
import com.datadrift.model.change.CreateTableChange.ColumnConfig;
import com.datadrift.model.change.CreateTableChange.ConstraintsConfig;
import com.datadrift.model.change.CreateTableChange.ForeignKeyAction;
import com.datadrift.model.change.CreateTableChange.UniqueConstraint;
import com.datadrift.model.change.CreateTableChange.TableCheckConstraint;
import com.datadrift.sql.dialect.SqlDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CreateTableExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SqlDialect sqlDialect;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    private CreateTableExecutor executor;

    @BeforeEach
    void setUp() {
        // Setup default dialect behavior - using lenient() for stubs that aren't always used
        lenient().when(sqlDialect.getName()).thenReturn("PostgreSQL");
        lenient().when(sqlDialect.supportsIfNotExists()).thenReturn(true);
        lenient().when(sqlDialect.getIfNotExistsSyntax()).thenReturn("IF NOT EXISTS ");
        lenient().when(sqlDialect.getAutoIncrementSyntax()).thenReturn("GENERATED ALWAYS AS IDENTITY");
        lenient().when(sqlDialect.getGeneratedColumnSyntax(anyString(), anyBoolean()))
                .thenAnswer(invocation -> {
                    String expr = invocation.getArgument(0);
                    return "GENERATED ALWAYS AS (" + expr + ") STORED";
                });
        lenient().when(sqlDialect.supportsTableComments()).thenReturn(true);
        lenient().when(sqlDialect.requiresSeparatedComments()).thenReturn(true);
        lenient().when(sqlDialect.getTableCommentSyntax(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String table = invocation.getArgument(0);
                    String comment = invocation.getArgument(1);
                    return "COMMENT ON TABLE " + table + " IS " + comment;
                });
        lenient().when(sqlDialect.getColumnCommentSyntax(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String table = invocation.getArgument(0);
                    String column = invocation.getArgument(1);
                    String comment = invocation.getArgument(2);
                    return "COMMENT ON COLUMN " + table + "." + column + " IS " + comment;
                });

        executor = new CreateTableExecutor(jdbcTemplate, sqlDialect);
    }

    @Test
    void testExecute_SimpleTable() {
        // Given
        CreateTableChange change = createSimpleTableChange();

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, atLeastOnce()).execute(anyString());
    }

    @Test
    void testGenerateSql_SimpleTable() {
        // Given
        CreateTableChange change = createSimpleTableChange();

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertTrue(sql.contains("CREATE TABLE"));
        assertTrue(sql.contains("\"users\""));
        assertTrue(sql.contains("\"id\" INTEGER"));
        assertTrue(sql.contains("\"name\" VARCHAR(100)"));
    }

    @Test
    void testGenerateSql_WithSchema() {
        // Given
        CreateTableChange change = createSimpleTableChange();
        change.setSchemaName("public");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"public\".\"users\""));
    }

    @Test
    void testGenerateSql_WithIfNotExists() {
        // Given
        CreateTableChange change = createSimpleTableChange();
        change.setIfNotExist(true);

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("IF NOT EXISTS"));
    }

    @Test
    void testGenerateSql_WithPrimaryKey() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("users");

        ColumnConfig idColumn = new ColumnConfig();
        idColumn.setName("id");
        idColumn.setType("INTEGER");
        ConstraintsConfig constraints = new ConstraintsConfig(false, true, false, null, null, null, null, null);
        idColumn.setConstraints(constraints);

        change.setColumns(List.of(idColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"id\" INTEGER"));
        assertTrue(sql.contains("PRIMARY KEY"));
    }

    @Test
    void testGenerateSql_WithCompositePrimaryKey() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("user_roles");

        ColumnConfig userIdColumn = createColumn("user_id", "INTEGER", false, false);
        ColumnConfig roleIdColumn = createColumn("role_id", "INTEGER", false, false);

        change.setColumns(List.of(userIdColumn, roleIdColumn));
        change.setPrimaryKeyColumns(List.of("user_id", "role_id"));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("PRIMARY KEY (\"user_id\", \"role_id\")"));
    }

    @Test
    void testGenerateSql_WithNotNullConstraint() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("users");

        ColumnConfig nameColumn = createColumn("name", "VARCHAR(100)", false, false);

        change.setColumns(List.of(nameColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"name\" VARCHAR(100) NOT NULL"));
    }

    @Test
    void testGenerateSql_WithUniqueConstraint() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("users");

        ColumnConfig emailColumn = new ColumnConfig();
        emailColumn.setName("email");
        emailColumn.setType("VARCHAR(255)");
        ConstraintsConfig constraints = new ConstraintsConfig(false, false, true, null, null, null, null, null);
        emailColumn.setConstraints(constraints);

        change.setColumns(List.of(emailColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("\"email\" VARCHAR(255)"));
        assertTrue(sql.contains("UNIQUE"));
    }

    @Test
    void testGenerateSql_WithDefaultValue() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("users");

        ColumnConfig statusColumn = new ColumnConfig();
        statusColumn.setName("status");
        statusColumn.setType("VARCHAR(20)");
        statusColumn.setDefaultValue("active");
        statusColumn.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(statusColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("DEFAULT 'active'"));
    }

    @Test
    void testGenerateSql_WithAutoIncrement() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("users");

        ColumnConfig idColumn = new ColumnConfig();
        idColumn.setName("id");
        idColumn.setType("INTEGER");
        idColumn.setAutoIncrement(true);
        idColumn.setConstraints(new ConstraintsConfig(false, true, null, null, null, null, null, null));

        change.setColumns(List.of(idColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("GENERATED ALWAYS AS IDENTITY"));
    }

    @Test
    void testGenerateSql_WithComputedColumn() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("products");

        ColumnConfig priceColumn = createColumn("price", "DECIMAL(10,2)", true, false);
        ColumnConfig taxColumn = new ColumnConfig();
        taxColumn.setName("tax");
        taxColumn.setType("DECIMAL(10,2)");
        taxColumn.setDefaultValueComputed("price * 0.15");
        taxColumn.setConstraints(new ConstraintsConfig(null, null, null, null, null, null, null, null));

        change.setColumns(List.of(priceColumn, taxColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("GENERATED ALWAYS AS (price * 0.15) STORED"));
    }

    @Test
    void testGenerateSql_WithForeignKey() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("orders");

        ColumnConfig userIdColumn = new ColumnConfig();
        userIdColumn.setName("user_id");
        userIdColumn.setType("INTEGER");
        ConstraintsConfig fkConstraints = new ConstraintsConfig(
                false, false, false,
                "fk_orders_users",
                "users(id)",
                null,
                ForeignKeyAction.CASCADE,
                null
        );
        userIdColumn.setConstraints(fkConstraints);

        change.setColumns(List.of(userIdColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("CONSTRAINT \"fk_orders_users\""));
        assertTrue(sql.contains("FOREIGN KEY (\"user_id\")"));
        assertTrue(sql.contains("REFERENCES users(id)"));
        assertTrue(sql.contains("ON DELETE CASCADE"));
    }

    @Test
    void testGenerateSql_WithTableLevelUniqueConstraint() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("users");

        ColumnConfig emailColumn = createColumn("email", "VARCHAR(255)", false, false);
        ColumnConfig usernameColumn = createColumn("username", "VARCHAR(100)", false, false);

        UniqueConstraint uniqueConstraint = new UniqueConstraint();
        uniqueConstraint.setConstraintName("uq_email_username");
        uniqueConstraint.setColumns(List.of("email", "username"));

        change.setColumns(List.of(emailColumn, usernameColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());
        change.setUniqueConstraints(List.of(uniqueConstraint));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("CONSTRAINT \"uq_email_username\""));
        assertTrue(sql.contains("UNIQUE (\"email\", \"username\")"));
    }

    @Test
    void testGenerateSql_WithTableLevelCheckConstraint() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("products");

        ColumnConfig priceColumn = createColumn("price", "DECIMAL(10,2)", true, false);

        TableCheckConstraint checkConstraint = new TableCheckConstraint();
        checkConstraint.setConstraintName("chk_positive_price");
        checkConstraint.setCheckExpression("price > 0");

        change.setColumns(List.of(priceColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());
        change.setCheckConstraints(List.of(checkConstraint));

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("CONSTRAINT \"chk_positive_price\""));
        assertTrue(sql.contains("CHECK (price > 0)"));
    }

    @Test
    void testGenerateSql_WithTableComment() {
        // Given
        CreateTableChange change = createSimpleTableChange();
        change.setRemarks("User accounts table");

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("COMMENT ON TABLE"));
        assertTrue(sql.contains("'User accounts table'"));
    }

    @Test
    void testGenerateSql_WithColumnComment() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("users");

        ColumnConfig idColumn = createColumn("id", "INTEGER", false, true);
        idColumn.setRemarks("Primary key");

        change.setColumns(List.of(idColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        // When
        String sql = executor.generateSql(change);

        // Then
        assertTrue(sql.contains("COMMENT ON COLUMN"));
        assertTrue(sql.contains("\"id\""));
        assertTrue(sql.contains("'Primary key'"));
    }

    @Test
    void testGenerateSql_ComplexTable() {
        // Given
        CreateTableChange change = new CreateTableChange();
        change.setTableName("orders");
        change.setSchemaName("sales");
        change.setIfNotExist(true);
        change.setRemarks("Customer orders");

        // ID column with auto-increment
        ColumnConfig idColumn = new ColumnConfig();
        idColumn.setName("id");
        idColumn.setType("INTEGER");
        idColumn.setAutoIncrement(true);
        idColumn.setConstraints(new ConstraintsConfig(false, true, null, null, null, null, null, null));
        idColumn.setRemarks("Order ID");

        // User ID with foreign key
        ColumnConfig userIdColumn = new ColumnConfig();
        userIdColumn.setName("user_id");
        userIdColumn.setType("INTEGER");
        userIdColumn.setConstraints(new ConstraintsConfig(
                false, false, false,
                "fk_orders_users",
                "users(id)",
                null,
                ForeignKeyAction.CASCADE,
                null
        ));

        // Total with check constraint
        ColumnConfig totalColumn = new ColumnConfig();
        totalColumn.setName("total");
        totalColumn.setType("DECIMAL(10,2)");
        totalColumn.setConstraints(new ConstraintsConfig(false, null, null, null, null, "total >= 0", null, null));

        // Status with default
        ColumnConfig statusColumn = new ColumnConfig();
        statusColumn.setName("status");
        statusColumn.setType("VARCHAR(20)");
        statusColumn.setDefaultValue("pending");
        statusColumn.setConstraints(new ConstraintsConfig(false, null, null, null, null, null, null, null));

        change.setColumns(List.of(idColumn, userIdColumn, totalColumn, statusColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        // When
        String sql = executor.generateSql(change);

        // Then
        assertNotNull(sql);
        assertTrue(sql.contains("IF NOT EXISTS"));
        assertTrue(sql.contains("\"sales\".\"orders\""));
        assertTrue(sql.contains("GENERATED ALWAYS AS IDENTITY"));
        assertTrue(sql.contains("FOREIGN KEY"));
        assertTrue(sql.contains("DEFAULT 'pending'"));
        assertTrue(sql.contains("CHECK (total >= 0)"));
        assertTrue(sql.contains("COMMENT ON TABLE"));
        assertTrue(sql.contains("COMMENT ON COLUMN"));
    }

    @Test
    void testExecute_MultipleStatements() {
        // Given
        CreateTableChange change = createSimpleTableChange();
        change.setRemarks("Test table");

        // When
        executor.execute(change);

        // Then
        verify(jdbcTemplate, atLeast(2)).execute(anyString());
    }

    private CreateTableChange createSimpleTableChange() {
        CreateTableChange change = new CreateTableChange();
        change.setTableName("users");

        ColumnConfig idColumn = createColumn("id", "INTEGER", false, true);
        ColumnConfig nameColumn = createColumn("name", "VARCHAR(100)", false, false);

        change.setColumns(List.of(idColumn, nameColumn));
        change.setPrimaryKeyColumns(new ArrayList<>());

        return change;
    }

    private ColumnConfig createColumn(String name, String type, boolean nullable, boolean primaryKey) {
        ColumnConfig column = new ColumnConfig();
        column.setName(name);
        column.setType(type);
        ConstraintsConfig constraints = new ConstraintsConfig(nullable, primaryKey, null, null, null, null, null, null);
        column.setConstraints(constraints);
        return column;
    }
}
