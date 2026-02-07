package com.datadrift.service;

import com.datadrift.exception.RollbackException;
import com.datadrift.executor.change.ChangeExecutor;
import com.datadrift.model.change.SqlChange;
import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.model.changelog.DatabaseChangeLog;
import com.datadrift.repository.ChangelogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RollbackServiceTest {

    @Mock
    private ChangelogRepository changelogRepository;

    @Mock
    private ChangelogParserService parserService;

    @Mock
    private ChangeExecutor<SqlChange> sqlExecutor;

    private RollbackService rollbackService;
    private Map<String, ChangeExecutor> executors;

    @BeforeEach
    void setUp() {
        executors = new HashMap<>();
        executors.put("sql", sqlExecutor);
        rollbackService = new RollbackService(changelogRepository, parserService, executors);
    }

    @Test
    void rollback_ZeroCount_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> rollbackService.rollback(0)
        );
    }

    @Test
    void rollback_NegativeCount_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> rollbackService.rollback(-1)
        );
    }

    @Test
    void rollback_NoExecutedChangesets_ReturnsZero() {
        when(changelogRepository.findLastN(1)).thenReturn(List.of());

        int result = rollbackService.rollback(1);

        assertEquals(0, result);
    }

    @Test
    void rollback_ChangesetNotFoundInParser_ThrowsException() {
        DatabaseChangeLog executed = createDatabaseChangeLog("001", "author1");
        when(changelogRepository.findLastN(1)).thenReturn(List.of(executed));
        when(parserService.parseAllChangelogs()).thenReturn(List.of());

        RollbackException exception = assertThrows(
                RollbackException.class,
                () -> rollbackService.rollback(1)
        );

        assertTrue(exception.getMessage().contains("Cannot find changeset definition"));
    }

    @Test
    void rollback_Success_ExecutesRollbackAndDeletes() {
        DatabaseChangeLog executed = createDatabaseChangeLog("001", "author1");
        when(changelogRepository.findLastN(1)).thenReturn(List.of(executed));

        ChangeSet changeSet = createChangeSetWithRollback("001", "author1");
        when(parserService.parseAllChangelogs()).thenReturn(List.of(changeSet));
        when(changelogRepository.delete("001", "author1")).thenReturn(1);

        int result = rollbackService.rollback(1);

        assertEquals(1, result);
        verify(sqlExecutor).execute(any(SqlChange.class));
        verify(changelogRepository).delete("001", "author1");
    }

    @Test
    void rollback_MultipleChangesets_RollsBackAll() {
        DatabaseChangeLog executed1 = createDatabaseChangeLog("002", "author1");
        DatabaseChangeLog executed2 = createDatabaseChangeLog("001", "author1");
        when(changelogRepository.findLastN(2)).thenReturn(List.of(executed1, executed2));

        ChangeSet changeSet1 = createChangeSetWithRollback("001", "author1");
        ChangeSet changeSet2 = createChangeSetWithRollback("002", "author1");
        when(parserService.parseAllChangelogs()).thenReturn(List.of(changeSet1, changeSet2));
        when(changelogRepository.delete(anyString(), anyString())).thenReturn(1);

        int result = rollbackService.rollback(2);

        assertEquals(2, result);
        verify(sqlExecutor, times(2)).execute(any(SqlChange.class));
        verify(changelogRepository, times(2)).delete(anyString(), anyString());
    }

    @Test
    void rollbackToTag_TagNotFound_ThrowsException() {
        when(changelogRepository.findByTag("v1.0")).thenReturn(List.of());

        RollbackException exception = assertThrows(
                RollbackException.class,
                () -> rollbackService.rollbackToTag("v1.0")
        );

        assertTrue(exception.getMessage().contains("No changeset found with tag"));
    }

    @Test
    void rollbackToTag_NoChangesetsAfterTag_ReturnsZero() {
        DatabaseChangeLog tagged = createDatabaseChangeLog("001", "author1");
        tagged.setOrderExecuted(5);
        when(changelogRepository.findByTag("v1.0")).thenReturn(List.of(tagged));
        when(changelogRepository.findAfterOrder(5)).thenReturn(List.of());

        int result = rollbackService.rollbackToTag("v1.0");

        assertEquals(0, result);
    }

    @Test
    void rollbackToTag_Success_RollsBackChangesAfterTag() {
        DatabaseChangeLog tagged = createDatabaseChangeLog("001", "author1");
        tagged.setOrderExecuted(5);
        when(changelogRepository.findByTag("v1.0")).thenReturn(List.of(tagged));

        DatabaseChangeLog afterTag = createDatabaseChangeLog("002", "author1");
        afterTag.setOrderExecuted(6);
        when(changelogRepository.findAfterOrder(5)).thenReturn(List.of(afterTag));

        ChangeSet changeSet = createChangeSetWithRollback("002", "author1");
        when(parserService.parseAllChangelogs()).thenReturn(List.of(changeSet));
        when(changelogRepository.delete("002", "author1")).thenReturn(1);

        int result = rollbackService.rollbackToTag("v1.0");

        assertEquals(1, result);
        verify(sqlExecutor).execute(any(SqlChange.class));
    }

    @Test
    void rollbackChangeSet_NoRollbackDefined_ThrowsException() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId("001");
        changeSet.setAuthor("author1");
        changeSet.setRollbackChanges(null);

        RollbackException exception = assertThrows(
                RollbackException.class,
                () -> rollbackService.rollbackChangeSet(changeSet)
        );

        assertTrue(exception.getMessage().contains("No rollback defined"));
    }

    @Test
    void rollbackChangeSet_EmptyRollback_ThrowsException() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId("001");
        changeSet.setAuthor("author1");
        changeSet.setRollbackChanges(List.of());

        assertThrows(
                RollbackException.class,
                () -> rollbackService.rollbackChangeSet(changeSet)
        );
    }

    @Test
    void rollbackChangeSet_Success_ExecutesAndDeletes() {
        ChangeSet changeSet = createChangeSetWithRollback("001", "author1");
        when(changelogRepository.delete("001", "author1")).thenReturn(1);

        rollbackService.rollbackChangeSet(changeSet);

        verify(sqlExecutor).execute(any(SqlChange.class));
        verify(changelogRepository).delete("001", "author1");
    }

    @Test
    void rollbackChangeSet_NoExecutorFound_ThrowsException() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId("001");
        changeSet.setAuthor("author1");

        SqlChange rollback = new SqlChange();
        rollback.setSql("DROP TABLE test");
        // Override the change type to something without an executor
        changeSet.setRollbackChanges(List.of(new com.datadrift.model.change.Change() {
            @Override
            public String getChangeType() {
                return "unknownType";
            }

            @Override
            public void validate() {
            }
        }));

        assertThrows(
                IllegalStateException.class,
                () -> rollbackService.rollbackChangeSet(changeSet)
        );
    }

    private DatabaseChangeLog createDatabaseChangeLog(String id, String author) {
        DatabaseChangeLog log = new DatabaseChangeLog();
        log.setId(id);
        log.setAuthor(author);
        return log;
    }

    private ChangeSet createChangeSetWithRollback(String id, String author) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId(id);
        changeSet.setAuthor(author);

        SqlChange rollbackChange = new SqlChange();
        rollbackChange.setSql("DROP TABLE test");
        changeSet.setRollbackChanges(List.of(rollbackChange));

        return changeSet;
    }
}
