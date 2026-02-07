package com.datadrift.repository;

import com.datadrift.model.changelog.DatabaseChangeLogLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Repository for DATABASECHANGELOGLOCK table.
 * Manages migration locks to prevent concurrent execution.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LockRepository {

    private static final int LOCK_ID = 1;
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DatabaseChangeLogLock> rowMapper = (rs, rowNum) -> {
        DatabaseChangeLogLock lock = new DatabaseChangeLogLock();
        lock.setId(rs.getInt("id"));
        lock.setLocked(rs.getBoolean("locked"));
        Timestamp lockGranted = rs.getTimestamp("lockgranted");
        lock.setLockGranted(lockGranted != null ? lockGranted.toLocalDateTime() : null);
        lock.setLockedBy(rs.getString("lockedby"));
        return lock;
    };

    public boolean acquireLock() {
        ensureLockTableExists();

        String lockedBy = getLockedByInfo();

        // Atomic update - only succeeds if lock is not currently held
        int updated = jdbcTemplate.update(
                "UPDATE DATABASECHANGELOGLOCK SET locked = true, lockgranted = ?, lockedby = ? " +
                        "WHERE id = ? AND locked = false",
                Timestamp.valueOf(LocalDateTime.now()),
                lockedBy,
                LOCK_ID
        );

        if (updated > 0) {
            log.info("Migration lock acquired by {}", lockedBy);
            return true;
        }

        DatabaseChangeLogLock currentLock = getLockStatus();
        if (currentLock != null) {
            log.warn("Migration lock already held by {} since {}",
                    currentLock.getLockedBy(), currentLock.getLockGranted());
        }
        return false;
    }

    public void releaseLock() {
        int updated = jdbcTemplate.update(
                "UPDATE DATABASECHANGELOGLOCK SET locked = false, lockgranted = NULL, lockedby = NULL WHERE id = ?",
                LOCK_ID
        );

        if (updated > 0) {
            log.info("Migration lock released");
        } else {
            log.warn("No lock to release (lock row not found)");
        }
    }

    public DatabaseChangeLogLock getLockStatus() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id, locked, lockgranted, lockedby FROM DATABASECHANGELOGLOCK WHERE id = ?",
                    rowMapper,
                    LOCK_ID
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void ensureLockTableExists() {
        // Create table if not exists
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS DATABASECHANGELOGLOCK (" +
                        "id INT PRIMARY KEY, " +
                        "locked BOOLEAN NOT NULL, " +
                        "lockgranted TIMESTAMP, " +
                        "lockedby VARCHAR(255))"
        );

        // Insert initial row if not exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DATABASECHANGELOGLOCK WHERE id = ?",
                Integer.class,
                LOCK_ID
        );

        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO DATABASECHANGELOGLOCK (id, locked, lockgranted, lockedby) VALUES (?, false, NULL, NULL)",
                    LOCK_ID
            );
            log.debug("Initialized DATABASECHANGELOGLOCK table");
        }
    }

    private String getLockedByInfo() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            long pid = ProcessHandle.current().pid();
            return hostname + " (" + pid + ")";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
