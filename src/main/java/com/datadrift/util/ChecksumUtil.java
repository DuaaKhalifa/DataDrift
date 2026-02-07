package com.datadrift.util;

import com.datadrift.model.change.Change;
import com.datadrift.model.changelog.ChangeSet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for calculating MD5 checksums of changesets.
 * Used for detecting modifications to already-executed changesets.
 */
public class ChecksumUtil {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    /**
     * Calculate MD5 checksum for a changeset.
     * Serializes the changeset to a consistent string format and computes MD5 hash.
     *
     * @param changeSet The changeset to calculate checksum for
     * @return MD5 hash as a hex string (e.g., "8:abc123def456...")
     */
    public static String calculateChecksum(ChangeSet changeSet) {
        if (changeSet == null) {
            return null;
        }

        String normalized = normalizeChangeSet(changeSet);
        byte[] hash = computeMd5(normalized);
        return "8:" + bytesToHex(hash);
    }

    /**
     * Serialize changeset to a consistent string representation.
     */
    private static String normalizeChangeSet(ChangeSet changeSet) {
        StringBuilder sb = new StringBuilder();

        // Include changeset metadata
        sb.append("id:").append(nullSafe(changeSet.getId())).append(";");
        sb.append("author:").append(nullSafe(changeSet.getAuthor())).append(";");

        // Include all changes in order
        if (changeSet.getChanges() != null) {
            for (Change change : changeSet.getChanges()) {
                sb.append("change:").append(change.getChangeType()).append(":");
                sb.append(normalizeChange(change)).append(";");
            }
        }

        // Include rollback changes
        if (changeSet.getRollbackChanges() != null) {
            for (Change change : changeSet.getRollbackChanges()) {
                sb.append("rollback:").append(change.getChangeType()).append(":");
                sb.append(normalizeChange(change)).append(";");
            }
        }

        return sb.toString();
    }

    /**
     * Normalize a single change to a string representation.
     * Uses toString() which should be consistent for same content.
     */
    private static String normalizeChange(Change change) {
        if (change == null) {
            return "";
        }
        // Use the change's toString representation
        // Each Change class should have a consistent toString via @Data
        // Would be Change(changeType=.., tableName=..,..)
        return change.toString();
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }

    private static byte[] computeMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        // each byte become two hex character
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            // change signed to unsigned (0xFF is 255)
            // Java bytes are signed (-128 to 127). This makes it unsigned (0 to 255).
            int v = bytes[i] & 0xFF;
            //A byte has 8 bits. Split it in half — first 4 bits become first hex char.
            //>>> 4 means "shift right 4 bits"
            hexChars[i * 2] = HEX_DIGITS[v >>> 4];
            //& 0x0F means "keep only the last 4 bits" — throws away the left half.
            hexChars[i * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(hexChars);
    }
}
