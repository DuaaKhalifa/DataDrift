package com.datadrift.util;

import com.datadrift.model.change.CreateTableChange;
import com.datadrift.model.change.SqlChange;
import com.datadrift.model.changelog.ChangeSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChecksumUtilTest {

    @Test
    void calculateChecksum_NullChangeSet_ReturnsNull() {
        assertNull(ChecksumUtil.calculateChecksum(null));
    }

    @Test
    void calculateChecksum_EmptyChangeSet_ReturnsChecksum() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId("001");
        changeSet.setAuthor("test");

        String checksum = ChecksumUtil.calculateChecksum(changeSet);

        assertNotNull(checksum);
        assertTrue(checksum.startsWith("8:"));
    }

    @Test
    void calculateChecksum_SameContent_ReturnsSameChecksum() {
        ChangeSet changeSet1 = createChangeSet("001", "author1");
        ChangeSet changeSet2 = createChangeSet("001", "author1");

        String checksum1 = ChecksumUtil.calculateChecksum(changeSet1);
        String checksum2 = ChecksumUtil.calculateChecksum(changeSet2);

        assertEquals(checksum1, checksum2);
    }

    @Test
    void calculateChecksum_DifferentId_ReturnsDifferentChecksum() {
        ChangeSet changeSet1 = createChangeSet("001", "author1");
        ChangeSet changeSet2 = createChangeSet("002", "author1");

        String checksum1 = ChecksumUtil.calculateChecksum(changeSet1);
        String checksum2 = ChecksumUtil.calculateChecksum(changeSet2);

        assertNotEquals(checksum1, checksum2);
    }

    @Test
    void calculateChecksum_DifferentAuthor_ReturnsDifferentChecksum() {
        ChangeSet changeSet1 = createChangeSet("001", "author1");
        ChangeSet changeSet2 = createChangeSet("001", "author2");

        String checksum1 = ChecksumUtil.calculateChecksum(changeSet1);
        String checksum2 = ChecksumUtil.calculateChecksum(changeSet2);

        assertNotEquals(checksum1, checksum2);
    }

    @Test
    void calculateChecksum_WithChanges_IncludesChangesInChecksum() {
        ChangeSet changeSet1 = createChangeSet("001", "author1");
        ChangeSet changeSet2 = createChangeSet("001", "author1");

        SqlChange sqlChange = new SqlChange();
        sqlChange.setSql("SELECT 1");
        changeSet2.setChanges(List.of(sqlChange));

        String checksum1 = ChecksumUtil.calculateChecksum(changeSet1);
        String checksum2 = ChecksumUtil.calculateChecksum(changeSet2);

        assertNotEquals(checksum1, checksum2);
    }

    @Test
    void calculateChecksum_DifferentChanges_ReturnsDifferentChecksum() {
        ChangeSet changeSet1 = createChangeSet("001", "author1");
        ChangeSet changeSet2 = createChangeSet("001", "author1");

        SqlChange sql1 = new SqlChange();
        sql1.setSql("SELECT 1");
        changeSet1.setChanges(List.of(sql1));

        SqlChange sql2 = new SqlChange();
        sql2.setSql("SELECT 2");
        changeSet2.setChanges(List.of(sql2));

        String checksum1 = ChecksumUtil.calculateChecksum(changeSet1);
        String checksum2 = ChecksumUtil.calculateChecksum(changeSet2);

        assertNotEquals(checksum1, checksum2);
    }

    @Test
    void calculateChecksum_WithRollbackChanges_IncludesRollbackInChecksum() {
        ChangeSet changeSet1 = createChangeSet("001", "author1");
        ChangeSet changeSet2 = createChangeSet("001", "author1");

        SqlChange rollback = new SqlChange();
        rollback.setSql("DROP TABLE test");
        changeSet2.setRollbackChanges(List.of(rollback));

        String checksum1 = ChecksumUtil.calculateChecksum(changeSet1);
        String checksum2 = ChecksumUtil.calculateChecksum(changeSet2);

        assertNotEquals(checksum1, checksum2);
    }

    @Test
    void calculateChecksum_ReturnsValidHexFormat() {
        ChangeSet changeSet = createChangeSet("001", "author1");

        String checksum = ChecksumUtil.calculateChecksum(changeSet);

        // Format is "8:" followed by 32 hex characters (MD5 = 128 bits = 32 hex chars)
        assertTrue(checksum.matches("8:[a-f0-9]{32}"));
    }

    private ChangeSet createChangeSet(String id, String author) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId(id);
        changeSet.setAuthor(author);
        return changeSet;
    }
}
