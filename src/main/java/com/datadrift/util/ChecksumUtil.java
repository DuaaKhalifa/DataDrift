package com.datadrift.util;

import com.datadrift.model.changelog.ChangeSet;

/**
 * Utility class for calculating MD5 checksums of changesets.
 * Used for detecting modifications to already-executed changesets.
 */
public class ChecksumUtil {

    /**
     * Calculate MD5 checksum for a changeset.
     *
     * Should:
     * 1. Serialize the changeset to a consistent string format
     * 2. Calculate MD5 hash of the string
     * 3. Return the hash as a hex string
     * 4. Ensure same changeset always produces same checksum
     */
    public static String calculateChecksum(ChangeSet changeSet) {
        // TODO: Implement checksum calculation
        return null;
    }
}
