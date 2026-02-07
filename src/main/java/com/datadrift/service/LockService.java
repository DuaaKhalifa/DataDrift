package com.datadrift.service;

import com.datadrift.exception.MigrationLockException;
import com.datadrift.model.changelog.DatabaseChangeLogLock;
import com.datadrift.repository.LockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for managing database migration locks.
 * Prevents concurrent migrations from running.
 */
@Slf4j
@Service
public class LockService {

    private final LockRepository lockRepository;
    private final int maxRetryAttempts;
    private final long retryDelayMs;

    public LockService(
            LockRepository lockRepository,
            @Value("${datadrift.lock.max-retry-attempts:5}") int maxRetryAttempts,
            @Value("${datadrift.lock.retry-delay-ms:1000}") long retryDelayMs) {
        this.lockRepository = lockRepository;
        this.maxRetryAttempts = maxRetryAttempts;
        this.retryDelayMs = retryDelayMs;
    }

    public void acquireLock() {
        log.debug("Attempting to acquire migration lock...");

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            if (lockRepository.acquireLock()) {
                log.info("Migration lock acquired successfully");
                return;
            }

            if (attempt < maxRetryAttempts) {
                log.debug("Lock acquisition attempt {} failed, retrying in {}ms...", attempt, retryDelayMs);
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new MigrationLockException("Lock acquisition interrupted");
                }
            }
        }

        // All retries exhausted
        DatabaseChangeLogLock lockStatus = lockRepository.getLockStatus();
        String lockedBy = lockStatus != null ? lockStatus.getLockedBy() : "unknown";
        throw new MigrationLockException(
                "Could not acquire migration lock after " + maxRetryAttempts +
                        " attempts. Lock is held by: " + lockedBy
        );
    }

    public void releaseLock() {
        log.debug("Releasing migration lock...");
        lockRepository.releaseLock();
    }

    public boolean isLocked() {
        DatabaseChangeLogLock status = lockRepository.getLockStatus();
        return status != null && Boolean.TRUE.equals(status.getLocked());
    }
}
