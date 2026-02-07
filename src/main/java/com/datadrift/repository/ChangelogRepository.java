package com.datadrift.repository;

import com.datadrift.model.changelog.DatabaseChangeLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * Repository for DATABASECHANGELOG table.
 * Tracks executed changesets.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChangelogRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DatabaseChangeLog> rowMapper = (rs, rowNum) -> {
        DatabaseChangeLog changeLog = new DatabaseChangeLog();
        changeLog.setId(rs.getString("id"));
        changeLog.setAuthor(rs.getString("author"));
        changeLog.setFilename(rs.getString("filename"));
        Timestamp dateExecuted = rs.getTimestamp("dateexecuted");
        changeLog.setDateExecuted(dateExecuted != null ? dateExecuted.toLocalDateTime() : null);
        changeLog.setOrderExecuted(rs.getInt("orderexecuted"));
        changeLog.setExecType(rs.getString("exectype"));
        changeLog.setMd5sum(rs.getString("md5sum"));
        changeLog.setDescription(rs.getString("description"));
        changeLog.setComments(rs.getString("comments"));
        changeLog.setTag(rs.getString("tag"));
        changeLog.setVersion(rs.getString("version"));
        changeLog.setContexts(rs.getString("contexts"));
        changeLog.setLabels(rs.getString("labels"));
        changeLog.setDeploymentId(rs.getString("deployment_id"));
        return changeLog;
    };

    public List<DatabaseChangeLog> findAll() {
        ensureChangeLogTableExists();
        return jdbcTemplate.query(
                "SELECT * FROM DATABASECHANGELOG ORDER BY orderexecuted",
                rowMapper
        );
    }

    public DatabaseChangeLog findByIdAndAuthor(String id, String author) {
        ensureChangeLogTableExists();
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM DATABASECHANGELOG WHERE id = ? AND author = ?",
                    rowMapper,
                    id, author
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public DatabaseChangeLog save(DatabaseChangeLog changeLog) {
        ensureChangeLogTableExists();

        jdbcTemplate.update(
                "INSERT INTO DATABASECHANGELOG " +
                        "(id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, " +
                        "description, comments, tag, version, contexts, labels, deployment_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                changeLog.getId(),
                changeLog.getAuthor(),
                changeLog.getFilename(),
                changeLog.getDateExecuted() != null ? Timestamp.valueOf(changeLog.getDateExecuted()) : null,
                changeLog.getOrderExecuted(),
                changeLog.getExecType(),
                changeLog.getMd5sum(),
                changeLog.getDescription(),
                changeLog.getComments(),
                changeLog.getTag(),
                changeLog.getVersion(),
                changeLog.getContexts(),
                changeLog.getLabels(),
                changeLog.getDeploymentId()
        );

        log.debug("Saved changelog entry: {}::{}", changeLog.getId(), changeLog.getAuthor());
        return changeLog;
    }

    public int delete(String id, String author) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM DATABASECHANGELOG WHERE id = ? AND author = ?",
                id, author
        );
        if (deleted > 0) {
            log.debug("Deleted changelog entry: {}::{}", id, author);
        }
        return deleted;
    }

    public List<DatabaseChangeLog> findByTag(String tag) {
        ensureChangeLogTableExists();
        return jdbcTemplate.query(
                "SELECT * FROM DATABASECHANGELOG WHERE tag = ? ORDER BY orderexecuted",
                rowMapper,
                tag
        );
    }

    public List<DatabaseChangeLog> findLastN(int count) {
        ensureChangeLogTableExists();
        return jdbcTemplate.query(
                "SELECT * FROM DATABASECHANGELOG ORDER BY orderexecuted DESC LIMIT ?",
                rowMapper,
                count
        );
    }

    public int getMaxOrderExecuted() {
        ensureChangeLogTableExists();
        Integer max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(orderexecuted), 0) FROM DATABASECHANGELOG",
                Integer.class
        );
        return max != null ? max : 0;
    }

    public List<DatabaseChangeLog> findAfterOrder(int orderExecuted) {
        ensureChangeLogTableExists();
        return jdbcTemplate.query(
                "SELECT * FROM DATABASECHANGELOG WHERE orderexecuted > ? ORDER BY orderexecuted DESC",
                rowMapper,
                orderExecuted
        );
    }

    private void ensureChangeLogTableExists() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS DATABASECHANGELOG (" +
                        "id VARCHAR(255) NOT NULL, " +
                        "author VARCHAR(255) NOT NULL, " +
                        "filename VARCHAR(255) NOT NULL, " +
                        "dateexecuted TIMESTAMP NOT NULL, " +
                        "orderexecuted INT NOT NULL, " +
                        "exectype VARCHAR(50) NOT NULL, " +
                        "md5sum VARCHAR(50), " +
                        "description VARCHAR(255), " +
                        "comments VARCHAR(255), " +
                        "tag VARCHAR(255), " +
                        "version VARCHAR(50), " +
                        "contexts VARCHAR(255), " +
                        "labels VARCHAR(255), " +
                        "deployment_id VARCHAR(50), " +
                        "PRIMARY KEY (id, author, filename))"
        );
    }
}
