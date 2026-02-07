package com.datadrift.service;

import com.datadrift.exception.ChecksumMismatchException;
import com.datadrift.exception.ValidationException;
import com.datadrift.model.change.Change;
import com.datadrift.model.change.SqlChange;
import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.model.changelog.DatabaseChangeLog;
import com.datadrift.repository.ChangelogRepository;
import com.datadrift.util.ChecksumUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private ChangelogRepository changelogRepository;

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService(changelogRepository);
    }

    @Test
    void validate_ValidChangeSet_NoException() {
        ChangeSet changeSet = createValidChangeSet("001", "author1");

        assertDoesNotThrow(() -> validationService.validate(List.of(changeSet)));
    }

    @Test
    void validate_EmptyList_NoException() {
        assertDoesNotThrow(() -> validationService.validate(List.of()));
    }

    @Test
    void validate_MissingId_ThrowsValidationException() {
        ChangeSet changeSet = createValidChangeSet(null, "author1");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validationService.validate(List.of(changeSet))
        );

        assertTrue(exception.getMessage().contains("Missing required field 'id'"));
    }

    @Test
    void validate_BlankId_ThrowsValidationException() {
        ChangeSet changeSet = createValidChangeSet("   ", "author1");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validationService.validate(List.of(changeSet))
        );

        assertTrue(exception.getMessage().contains("Missing required field 'id'"));
    }

    @Test
    void validate_MissingAuthor_ThrowsValidationException() {
        ChangeSet changeSet = createValidChangeSet("001", null);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validationService.validate(List.of(changeSet))
        );

        assertTrue(exception.getMessage().contains("Missing required field 'author'"));
    }

    @Test
    void validate_NoChanges_ThrowsValidationException() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId("001");
        changeSet.setAuthor("author1");
        changeSet.setChanges(null);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validationService.validate(List.of(changeSet))
        );

        assertTrue(exception.getMessage().contains("No changes defined"));
    }

    @Test
    void validate_EmptyChanges_ThrowsValidationException() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId("001");
        changeSet.setAuthor("author1");
        changeSet.setChanges(new ArrayList<>());

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validationService.validate(List.of(changeSet))
        );

        assertTrue(exception.getMessage().contains("No changes defined"));
    }

    @Test
    void validate_DuplicateChangeSetId_ThrowsValidationException() {
        ChangeSet changeSet1 = createValidChangeSet("001", "author1");
        ChangeSet changeSet2 = createValidChangeSet("001", "author1");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validationService.validate(List.of(changeSet1, changeSet2))
        );

        assertTrue(exception.getMessage().contains("Duplicate changeset id"));
    }

    @Test
    void validate_SameIdDifferentAuthor_NoException() {
        ChangeSet changeSet1 = createValidChangeSet("001", "author1");
        ChangeSet changeSet2 = createValidChangeSet("001", "author2");

        assertDoesNotThrow(() -> validationService.validate(List.of(changeSet1, changeSet2)));
    }

    @Test
    void validate_InvalidChange_ThrowsValidationException() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId("001");
        changeSet.setAuthor("author1");

        Change invalidChange = mock(Change.class);
        when(invalidChange.getChangeType()).thenReturn("testChange");
        doThrow(new IllegalArgumentException("tableName is required"))
                .when(invalidChange).validate();

        changeSet.setChanges(List.of(invalidChange));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validationService.validate(List.of(changeSet))
        );

        assertTrue(exception.getMessage().contains("tableName is required"));
    }

    @Test
    void validate_InvalidRollbackChange_ThrowsValidationException() {
        ChangeSet changeSet = createValidChangeSet("001", "author1");

        Change invalidRollback = mock(Change.class);
        when(invalidRollback.getChangeType()).thenReturn("dropTable");
        doThrow(new IllegalArgumentException("tableName is required"))
                .when(invalidRollback).validate();

        changeSet.setRollbackChanges(List.of(invalidRollback));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validationService.validate(List.of(changeSet))
        );

        assertTrue(exception.getMessage().contains("Rollback Change"));
        assertTrue(exception.getMessage().contains("tableName is required"));
    }

    @Test
    void validate_MultipleErrors_ReportsAll() {
        ChangeSet changeSet1 = createValidChangeSet(null, null);
        ChangeSet changeSet2 = createValidChangeSet("002", null);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validationService.validate(List.of(changeSet1, changeSet2))
        );

        // Should report multiple errors
        assertTrue(exception.getMessage().contains("error(s)"));
    }

    @Test
    void validateChecksums_NotExecuted_NoException() {
        ChangeSet changeSet = createValidChangeSet("001", "author1");
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(null);

        assertDoesNotThrow(() -> validationService.validateChecksums(List.of(changeSet)));
    }

    @Test
    void validateChecksums_MatchingChecksum_NoException() {
        ChangeSet changeSet = createValidChangeSet("001", "author1");
        String checksum = ChecksumUtil.calculateChecksum(changeSet);

        DatabaseChangeLog executed = new DatabaseChangeLog();
        executed.setMd5sum(checksum);
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(executed);

        assertDoesNotThrow(() -> validationService.validateChecksums(List.of(changeSet)));
    }

    @Test
    void validateChecksums_MismatchedChecksum_ThrowsException() {
        ChangeSet changeSet = createValidChangeSet("001", "author1");
        changeSet.setFilename("test.yaml");

        DatabaseChangeLog executed = new DatabaseChangeLog();
        executed.setMd5sum("8:differentchecksum12345678901234");
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(executed);

        ChecksumMismatchException exception = assertThrows(
                ChecksumMismatchException.class,
                () -> validationService.validateChecksums(List.of(changeSet))
        );

        assertTrue(exception.getMessage().contains("001::author1"));
        assertTrue(exception.getMessage().contains("was modified after execution"));
    }

    @Test
    void validateChecksums_NullStoredChecksum_NoException() {
        ChangeSet changeSet = createValidChangeSet("001", "author1");

        DatabaseChangeLog executed = new DatabaseChangeLog();
        executed.setMd5sum(null);
        when(changelogRepository.findByIdAndAuthor("001", "author1")).thenReturn(executed);

        assertDoesNotThrow(() -> validationService.validateChecksums(List.of(changeSet)));
    }

    private ChangeSet createValidChangeSet(String id, String author) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setId(id);
        changeSet.setAuthor(author);

        SqlChange sqlChange = new SqlChange();
        sqlChange.setSql("SELECT 1");
        changeSet.setChanges(List.of(sqlChange));

        return changeSet;
    }
}
