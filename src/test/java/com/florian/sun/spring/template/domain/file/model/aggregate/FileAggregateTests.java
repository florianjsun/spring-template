package com.florian.sun.spring.template.domain.file.model.aggregate;

import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.domain.file.model.entity.FileEntity;
import com.florian.sun.spring.template.domain.file.model.enums.FileErrorCode;
import com.florian.sun.spring.template.domain.file.model.param.CreateFileParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileAggregateTests {

    private static final String STORAGE_KEY_PATTERN = "^\\d{4}/\\d{2}/\\d{2}/\\d+(\\.[a-z0-9]+)?$";

    @Test
    void createStripsClientPathAndBuildsStorageKey() {
        FileAggregate aggregate = FileAggregate.create(param("C:\\Users\\alice\\Report.PDF", "application/pdf", 1024L, 1L));
        FileEntity file = aggregate.getFile();

        assertTrue(aggregate.isNew());
        assertEquals("Report.PDF", file.getOriginalName());
        assertTrue(file.getStorageKey().matches(STORAGE_KEY_PATTERN), file.getStorageKey());
        assertTrue(file.getStorageKey().endsWith(".pdf"));
        assertEquals("application/pdf", file.getContentType());
        assertEquals(1024L, file.getFileSize());
        assertEquals(1L, file.getUploaderId());
    }

    @Test
    void createHandlesMissingExtensionAndBlankContentType() {
        FileAggregate aggregate = FileAggregate.create(param("README", " ", 10L, 1L));
        FileEntity file = aggregate.getFile();

        assertTrue(file.getStorageKey().matches(STORAGE_KEY_PATTERN), file.getStorageKey());
        assertFalse(file.getStorageKey().contains("."));
        assertEquals("application/octet-stream", file.getContentType());
    }

    @Test
    void storageKeysAreUniquePerFile() {
        FileAggregate first = FileAggregate.create(param("a.txt", "text/plain", 1L, 1L));
        FileAggregate second = FileAggregate.create(param("a.txt", "text/plain", 1L, 1L));

        assertFalse(first.getFile().getStorageKey().equals(second.getFile().getStorageKey()));
    }

    @Test
    void createRejectsBlankName() {
        BizException exception = assertThrows(BizException.class,
                () -> FileAggregate.create(param("  ", "text/plain", 1L, 1L)));

        assertEquals(FileErrorCode.FILE_NAME_BLANK.getCode(), exception.getCode());
    }

    @Test
    void createRejectsEmptyFile() {
        BizException zero = assertThrows(BizException.class,
                () -> FileAggregate.create(param("a.txt", "text/plain", 0L, 1L)));
        BizException missing = assertThrows(BizException.class,
                () -> FileAggregate.create(param("a.txt", "text/plain", null, 1L)));

        assertEquals(FileErrorCode.FILE_EMPTY.getCode(), zero.getCode());
        assertEquals(FileErrorCode.FILE_EMPTY.getCode(), missing.getCode());
    }

    @Test
    void ownershipIsCheckedAgainstUploader() {
        FileAggregate aggregate = FileAggregate.create(param("a.txt", "text/plain", 1L, 1L));

        assertTrue(aggregate.isOwnedBy(1L));
        assertFalse(aggregate.isOwnedBy(2L));
        assertDoesNotThrow(() -> aggregate.ensureOwnedBy(1L));

        BizException exception = assertThrows(BizException.class, () -> aggregate.ensureOwnedBy(2L));
        assertEquals(FileErrorCode.NOT_FILE_OWNER.getCode(), exception.getCode());
    }

    private static CreateFileParam param(String originalName, String contentType, Long fileSize, Long uploaderId) {
        CreateFileParam param = new CreateFileParam();
        param.setOriginalName(originalName);
        param.setContentType(contentType);
        param.setFileSize(fileSize);
        param.setUploaderId(uploaderId);
        return param;
    }
}
