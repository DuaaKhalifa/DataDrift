package com.datadrift.service;

import com.datadrift.exception.ChangeSetExecutionException;
import com.datadrift.executor.change.ChangeExecutor;
import com.datadrift.model.change.Change;
import com.datadrift.model.change.SqlChange;
import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.model.changelog.DatabaseChangeLog;
import com.datadrift.repository.ChangelogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangelogExecutorServiceTest {

    @Mock
    private ChangelogRepository changelogRepository;

    @Mock
    private ChangeExecutor<SqlChange> sqlExecutor;

    private ChangelogExecutorService executorService;
    private Map<String, ChangeExecutor> executors;

    @BeforeEach
    void setUp() {
        executors = new HashMap<>();
        executors.put("sql", sqlExecutor);
        executorService = new ChangelogExecutorService(changelogRepository, executors);
    }

    @Test
    void filterPendingChangeSets_NotExecuted_ReturnsPending() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(null);

        List<ChangeSet> pending = executorService.filterPendingChangeSets(List.of(changeSet));

        assertEquals(1, pending.size());
        assertEquals("001", pending.get(0).getId());
    }

    @Test
    void filterPendingChangeSets_AlreadyExecuted_FiltersOut() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        DatabaseChangeLog executed = new DatabaseChangeLog();
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(executed);

        List<ChangeSet> pending = executorService.filterPendingChangeSets(List.of(changeSet));

        assertTrue(pending.isEmpty());
    }

    @Test
    void filterPendingChangeSets_RunAlways_AlwaysIncluded() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        changeSet.setRunAlways(true);

        DatabaseChangeLog executed = new DatabaseChangeLog();
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(executed);

        List<ChangeSet> pending = executorService.filterPendingChangeSets(List.of(changeSet));

        assertEquals(1, pending.size());
    }

    @Test
    void filterPendingChangeSets_RunOnChange_ChecksumChanged_Included() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        changeSet.setRunOnChange(true);

        DatabaseChangeLog executed = new DatabaseChangeLog();
        executed.setMd5sum("8:oldchecksum1234567890123456");
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(executed);

        List<ChangeSet> pending = executorService.filterPendingChangeSets(List.of(changeSet));

        assertEquals(1, pending.size());
    }

    @Test
    void filterPendingChangeSets_RunOnChange_ChecksumSame_FiltersOut() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        changeSet.setRunOnChange(true);

        DatabaseChangeLog executed = new DatabaseChangeLog();
        executed.setMd5sum(com.datadrift.util.ChecksumUtil.calculateChecksum(changeSet));
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(executed);

        List<ChangeSet> pending = executorService.filterPendingChangeSets(List.of(changeSet));

        assertTrue(pending.isEmpty());
    }

    @Test
    void executePendingChangeSets_NoPending_ReturnsZero() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        DatabaseChangeLog executed = new DatabaseChangeLog();
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(executed);

        int count = executorService.executePendingChangeSets(List.of(changeSet));

        assertEquals(0, count);
        verify(sqlExecutor, never()).execute(any());
    }

    @Test
    void executePendingChangeSets_HasPending_ExecutesAndRecords() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(null);
        when(changelogRepository.getMaxOrderExecuted()).thenReturn(0);

        int count = executorService.executePendingChangeSets(List.of(changeSet));

        assertEquals(1, count);
        verify(sqlExecutor).execute(any(SqlChange.class));
        verify(changelogRepository).save(any(DatabaseChangeLog.class));
    }

    @Test
    void executePendingChangeSets_RecordsCorrectMetadata() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        changeSet.setFilename("test.yaml");
        changeSet.setComment("Test comment");
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(null);
        when(changelogRepository.getMaxOrderExecuted()).thenReturn(5);

        executorService.executePendingChangeSets(List.of(changeSet));

        ArgumentCaptor<DatabaseChangeLog> captor = ArgumentCaptor.forClass(DatabaseChangeLog.class);
        verify(changelogRepository).save(captor.capture());

        DatabaseChangeLog saved = captor.getValue();
        assertEquals("001", saved.getId());
        assertEquals("author1", saved.getAuthor());
        assertEquals("test.yaml", saved.getFilename());
        assertEquals(6, saved.getOrderExecuted());
        assertEquals("EXECUTED", saved.getExecType());
        assertNotNull(saved.getMd5sum());
        assertNotNull(saved.getDateExecuted());
    }

    @Test
    void executeChangeSet_Success_RecordsExecuted() {
        ChangeSet changeSet = createChangeSet("001", "author1");

        executorService.executeChangeSet(changeSet, 1, "deploy-123");

        verify(sqlExecutor).execute(any(SqlChange.class));

        ArgumentCaptor<DatabaseChangeLog> captor = ArgumentCaptor.forClass(DatabaseChangeLog.class);
        verify(changelogRepository).save(captor.capture());
        assertEquals("EXECUTED", captor.getValue().getExecType());
    }

    @Test
    void executeChangeSet_Failure_FailOnErrorTrue_ThrowsException() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        changeSet.setFailOnError(true);

        doThrow(new RuntimeException("SQL error")).when(sqlExecutor).execute(any());

        assertThrows(
                ChangeSetExecutionException.class,
                () -> executorService.executeChangeSet(changeSet, 1, "deploy-123")
        );

        ArgumentCaptor<DatabaseChangeLog> captor = ArgumentCaptor.forClass(DatabaseChangeLog.class);
        verify(changelogRepository).save(captor.capture());
        assertEquals("FAILED", captor.getValue().getExecType());
    }

    @Test
    void executeChangeSet_Failure_FailOnErrorFalse_RecordsMarkRan() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        changeSet.setFailOnError(false);

        doThrow(new RuntimeException("SQL error")).when(sqlExecutor).execute(any());

        assertDoesNotThrow(() -> executorService.executeChangeSet(changeSet, 1, "deploy-123"));

        ArgumentCaptor<DatabaseChangeLog> captor = ArgumentCaptor.forClass(DatabaseChangeLog.class);
        verify(changelogRepository).save(captor.capture());
        assertEquals("MARK_RAN", captor.getValue().getExecType());
    }

    @Test
    void executeChangeSet_NoExecutorFound_ThrowsException() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId("001");
        changeSet.setAuthor("author1");

        Change unknownChange = mock(Change.class);
        when(unknownChange.getChangeType()).thenReturn("unknownType");
        changeSet.setChanges(List.of(unknownChange));

        // IllegalStateException gets wrapped in ChangeSetExecutionException
        ChangeSetExecutionException exception = assertThrows(
                ChangeSetExecutionException.class,
                () -> executorService.executeChangeSet(changeSet, 1, "deploy-123")
        );

        assertTrue(exception.getCause() instanceof IllegalStateException);
    }

    @Test
    void generateSql_GeneratesSqlFromExecutor() {
        ChangeSet changeSet = createChangeSet("001", "author1");
        when(sqlExecutor.generateSql(any(SqlChange.class))).thenReturn("SELECT 1");

        String sql = executorService.generateSql(changeSet);

        assertTrue(sql.contains("-- ChangeSet: 001::author1"));
        assertTrue(sql.contains("SELECT 1"));
    }

    @Test
    void generateSql_NoExecutor_IncludesErrorComment() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId("001");
        changeSet.setAuthor("author1");

        Change unknownChange = mock(Change.class);
        when(unknownChange.getChangeType()).thenReturn("unknownType");
        changeSet.setChanges(List.of(unknownChange));

        String sql = executorService.generateSql(changeSet);

        assertTrue(sql.contains("ERROR: No executor for unknownType"));
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
