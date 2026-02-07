package com.datadrift.service;

import com.datadrift.exception.MigrationLockException;
import com.datadrift.model.changelog.DatabaseChangeLogLock;
import com.datadrift.repository.LockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {

    @Mock
    private LockRepository lockRepository;

    private LockService lockService;

    @BeforeEach
    void setUp() {
        // maxRetryAttempts=3, retryDelayMs=10 (fast for tests)
        lockService = new LockService(lockRepository, 3, 10);
    }

    @Test
    void acquireLock_Success_OnFirstAttempt() {
        when(lockRepository.acquireLock()).thenReturn(true);

        assertDoesNotThrow(() -> lockService.acquireLock());

        verify(lockRepository, times(1)).acquireLock();
    }

    @Test
    void acquireLock_Success_OnSecondAttempt() {
        when(lockRepository.acquireLock())
                .thenReturn(false)
                .thenReturn(true);

        assertDoesNotThrow(() -> lockService.acquireLock());

        verify(lockRepository, times(2)).acquireLock();
    }

    @Test
    void acquireLock_Failure_AfterAllRetries() {
        when(lockRepository.acquireLock()).thenReturn(false);
        DatabaseChangeLogLock lockStatus = new DatabaseChangeLogLock();
        lockStatus.setLockedBy("other-host (1234)");
        when(lockRepository.getLockStatus()).thenReturn(lockStatus);

        MigrationLockException exception = assertThrows(
                MigrationLockException.class,
                () -> lockService.acquireLock()
        );

        assertTrue(exception.getMessage().contains("other-host (1234)"));
        verify(lockRepository, times(3)).acquireLock();
    }

    @Test
    void acquireLock_Failure_NullLockStatus() {
        when(lockRepository.acquireLock()).thenReturn(false);
        when(lockRepository.getLockStatus()).thenReturn(null);

        MigrationLockException exception = assertThrows(
                MigrationLockException.class,
                () -> lockService.acquireLock()
        );

        assertTrue(exception.getMessage().contains("unknown"));
    }

    @Test
    void releaseLock_CallsRepository() {
        lockService.releaseLock();

        verify(lockRepository).releaseLock();
    }

    @Test
    void isLocked_True_WhenLockStatusIsLocked() {
        DatabaseChangeLogLock lockStatus = new DatabaseChangeLogLock();
        lockStatus.setLocked(true);
        when(lockRepository.getLockStatus()).thenReturn(lockStatus);

        assertTrue(lockService.isLocked());
    }

    @Test
    void isLocked_False_WhenLockStatusIsNotLocked() {
        DatabaseChangeLogLock lockStatus = new DatabaseChangeLogLock();
        lockStatus.setLocked(false);
        when(lockRepository.getLockStatus()).thenReturn(lockStatus);

        assertFalse(lockService.isLocked());
    }

    @Test
    void isLocked_False_WhenLockStatusIsNull() {
        when(lockRepository.getLockStatus()).thenReturn(null);

        assertFalse(lockService.isLocked());
    }
}
