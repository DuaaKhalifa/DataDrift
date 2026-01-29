package com.datadrift.model.change;

import lombok.Data;

/**
 * Attaches a UNIQUE or PRIMARY KEY constraint to an existing index.
 *
 * This enables a zero-downtime approach for adding constraints to large tables:
 * 1. CREATE UNIQUE INDEX CONCURRENTLY (slow, but non-blocking)
 * 2. ADD CONSTRAINT USING INDEX (instant)
 *
 * Example:
 *   CREATE UNIQUE INDEX CONCURRENTLY idx_email ON users(email);
 *   ALTER TABLE users ADD CONSTRAINT uq_email UNIQUE USING INDEX idx_email;
 *
 * After attachment, the index becomes owned by the constraint:
 * - Dropping the constraint will also drop the index.
 * - Dropping the index directly will error, unless CASCADE is used,
 *   which drops both the index and the constraint.
 */
@Data
public class DropIndexChange implements Change {
    private String indexName;
    private String schemaName;
    private Boolean cascade;
    private Boolean ifExists;

    @Override
    public String getChangeType() {
        return "dropIndex";
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (indexName == null || indexName.isBlank()) {
            throw new IllegalArgumentException("indexName is required for dropIndex");
        }
    }
}
