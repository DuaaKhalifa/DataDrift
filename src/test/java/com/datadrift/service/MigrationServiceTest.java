package com.datadrift.service;

import com.datadrift.model.MigrationStatus;
import com.datadrift.model.change.SqlChange;
import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.model.changelog.DatabaseChangeLog;
import com.datadrift.repository.ChangelogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {

    @Mock
    private ChangelogParserService parserService;

    @Mock
    private ChangelogExecutorService executorService;

    @Mock
    private ValidationService validationService;

    @Mock
    private LockService lockService;

    @Mock
    private ChangelogRepository changelogRepository;

    private MigrationService migrationService;

    @BeforeEach
    void setUp() {
        migrationService = new MigrationService(
                parserService,
                executorService,
                validationService,
                lockService,
                changelogRepository
        );
    }

    @Test
    void migrate_AcquiresAndReleasesLock() {
        when(parserService.parseAllChangelogs()).thenReturn(List.of());

        migrationService.migrate();

        InOrder inOrder = inOrder(lockService);
        inOrder.verify(lockService).acquireLock();
        inOrder.verify(lockService).releaseLock();
    }

    @Test
    void migrate_ReleasesLockEvenOnException() {
        when(parserService.parseAllChangelogs()).thenThrow(new RuntimeException("Parse error"));

        assertThrows(RuntimeException.class, () -> migrationService.migrate());

        verify(lockService).acquireLock();
        verify(lockService).releaseLock();
    }

    @Test
    void migrate_NoChangesets_ReturnsZero() {
        when(parserService.parseAllChangelogs()).thenReturn(List.of());

        int result = migrationService.migrate();

        assertEquals(0, result);
        verify(executorService, never()).executePendingChangeSets(any());
    }

    @Test
    void migrate_ValidatesChangesets() {
        List<ChangeSet> changeSets = List.of(createChangeSet("001", "author1"));
        when(parserService.parseAllChangelogs()).thenReturn(changeSets);
        when(executorService.executePendingChangeSets(changeSets)).thenReturn(1);

        migrationService.migrate();

        verify(validationService).validate(changeSets);
        verify(validationService).validateChecksums(changeSets);
    }

    @Test
    void migrate_ExecutesPendingChangesets() {
        List<ChangeSet> changeSets = List.of(createChangeSet("001", "author1"));
        when(parserService.parseAllChangelogs()).thenReturn(changeSets);
        when(executorService.executePendingChangeSets(changeSets)).thenReturn(1);

        int result = migrationService.migrate();

        assertEquals(1, result);
        verify(executorService).executePendingChangeSets(changeSets);
    }

    @Test
    void migrate_ValidationFails_DoesNotExecute() {
        List<ChangeSet> changeSets = List.of(createChangeSet("001", "author1"));
        when(parserService.parseAllChangelogs()).thenReturn(changeSets);
        doThrow(new RuntimeException("Validation failed")).when(validationService).validate(changeSets);

        assertThrows(RuntimeException.class, () -> migrationService.migrate());

        verify(executorService, never()).executePendingChangeSets(any());
    }

    @Test
    void getStatus_ReturnsCorrectCounts() {
        ChangeSet pending1 = createChangeSet("002", "author1");
        ChangeSet pending2 = createChangeSet("003", "author1");
        List<ChangeSet> allChangeSets = List.of(
                createChangeSet("001", "author1"),
                pending1,
                pending2
        );

        DatabaseChangeLog executed = new DatabaseChangeLog();
        executed.setDateExecuted(LocalDateTime.now());
        List<DatabaseChangeLog> executedLogs = List.of(executed);

        when(parserService.parseAllChangelogs()).thenReturn(allChangeSets);
        when(changelogRepository.findAll()).thenReturn(executedLogs);
        when(executorService.filterPendingChangeSets(allChangeSets)).thenReturn(List.of(pending1, pending2));
        when(lockService.isLocked()).thenReturn(false);

        MigrationStatus status = migrationService.getStatus();

        assertEquals(3, status.getTotalChangesets());
        assertEquals(1, status.getExecutedCount());
        assertEquals(2, status.getPendingCount());
        assertEquals(2, status.getPendingChangesets().size());
        assertFalse(status.isLocked());
        assertNotNull(status.getLastExecutionTime());
    }

    @Test
    void getStatus_NoPendingChangesets() {
        List<ChangeSet> allChangeSets = List.of(createChangeSet("001", "author1"));

        DatabaseChangeLog executedLog = new DatabaseChangeLog();
        executedLog.setDateExecuted(LocalDateTime.now());

        when(parserService.parseAllChangelogs()).thenReturn(allChangeSets);
        when(changelogRepository.findAll()).thenReturn(List.of(executedLog));
        when(executorService.filterPendingChangeSets(allChangeSets)).thenReturn(List.of());
        when(lockService.isLocked()).thenReturn(false);

        MigrationStatus status = migrationService.getStatus();

        assertEquals(0, status.getPendingCount());
        assertTrue(status.getPendingChangesets().isEmpty());
    }

    @Test
    void getStatus_NoExecutedChangesets_LastExecutionTimeIsNull() {
        when(parserService.parseAllChangelogs()).thenReturn(List.of());
        when(changelogRepository.findAll()).thenReturn(List.of());
        when(executorService.filterPendingChangeSets(any())).thenReturn(List.of());
        when(lockService.isLocked()).thenReturn(false);

        MigrationStatus status = migrationService.getStatus();

        assertNull(status.getLastExecutionTime());
    }

    @Test
    void getStatus_LockedStatus() {
        when(parserService.parseAllChangelogs()).thenReturn(List.of());
        when(changelogRepository.findAll()).thenReturn(List.of());
        when(executorService.filterPendingChangeSets(any())).thenReturn(List.of());
        when(lockService.isLocked()).thenReturn(true);

        MigrationStatus status = migrationService.getStatus();

        assertTrue(status.isLocked());
    }

    @Test
    void generateSql_NoPendingChangesets_ReturnsMessage() {
        when(parserService.parseAllChangelogs()).thenReturn(List.of());
        when(executorService.filterPendingChangeSets(any())).thenReturn(List.of());

        String sql = migrationService.generateSql();

        assertEquals("-- No pending changesets", sql);
    }

    @Test
    void generateSql_HasPendingChangesets_GeneratesSql() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        when(parserService.parseAllChangelogs()).thenReturn(List.of(changeSet));
        when(executorService.filterPendingChangeSets(any())).thenReturn(List.of(changeSet));
        when(executorService.generateSql(changeSet)).thenReturn("-- SQL for 001\nCREATE TABLE test;");

        String sql = migrationService.generateSql();

        assertTrue(sql.contains("DataDrift Migration SQL Preview"));
        assertTrue(sql.contains("Pending changesets: 1"));
        assertTrue(sql.contains("CREATE TABLE test"));
    }

    @Test
    void generateSql_MultipleChangesets_CombinesAll() {
        ChangeSet changeSet1 = createChangeSet("001", "author1");
        ChangeSet changeSet2 = createChangeSet("002", "author1");
        List<ChangeSet> pending = List.of(changeSet1, changeSet2);

        when(parserService.parseAllChangelogs()).thenReturn(pending);
        when(executorService.filterPendingChangeSets(any())).thenReturn(pending);
        when(executorService.generateSql(changeSet1)).thenReturn("-- SQL 1");
        when(executorService.generateSql(changeSet2)).thenReturn("-- SQL 2");

        String sql = migrationService.generateSql();

        assertTrue(sql.contains("-- SQL 1"));
        assertTrue(sql.contains("-- SQL 2"));
    }

    private ChangeSet createChangeSet(String id, String author) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId(id);
        changeSet.setAuthor(author);

        SqlChange sqlChange = new SqlChange();
        sqlChange.setSql("SELECT 1");
        changeSet.setChanges(List.of(sqlChange));

        return changeSet;
    }
}
