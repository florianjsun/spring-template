package com.florian.sun.spring.template.domain.file.service;

import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.domain.file.model.aggregate.FileAggregate;
import com.florian.sun.spring.template.domain.file.model.enums.FileErrorCode;
import com.florian.sun.spring.template.domain.file.model.param.CreateFileParam;
import com.florian.sun.spring.template.domain.file.model.param.RemoveFileParam;
import com.florian.sun.spring.template.domain.file.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileDomainServiceTests {

    private FileRepository fileRepository;
    private FileDomainService fileDomainService;

    @BeforeEach
    void setUp() {
        fileRepository = mock(FileRepository.class);
        fileDomainService = new FileDomainService(fileRepository);
    }

    @Test
    void createFileSavesAndReturnsAggregate() {
        CreateFileParam param = new CreateFileParam();
        param.setOriginalName("a.txt");
        param.setContentType("text/plain");
        param.setFileSize(3L);
        param.setUploaderId(1L);

        FileAggregate created = fileDomainService.createFile(param);

        assertEquals("a.txt", created.getFile().getOriginalName());
        verify(fileRepository).save(created);
    }

    @Test
    void removeFileRejectsUnknownFile() {
        when(fileRepository.findById(404L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class,
                () -> fileDomainService.removeFile(removeParam(404L, 1L)));

        assertEquals(FileErrorCode.FILE_NOT_FOUND.getCode(), exception.getCode());
        verify(fileRepository, never()).remove(any());
    }

    @Test
    void removeFileRejectsNonOwner() {
        FileAggregate existing = existingFile(1L);
        when(fileRepository.findById(10L)).thenReturn(Optional.of(existing));

        BizException exception = assertThrows(BizException.class,
                () -> fileDomainService.removeFile(removeParam(10L, 2L)));

        assertEquals(FileErrorCode.NOT_FILE_OWNER.getCode(), exception.getCode());
        verify(fileRepository, never()).remove(any());
    }

    @Test
    void removeFileRemovesAndReturnsAggregateForOwner() {
        FileAggregate existing = existingFile(1L);
        when(fileRepository.findById(10L)).thenReturn(Optional.of(existing));

        FileAggregate removed = fileDomainService.removeFile(removeParam(10L, 1L));

        assertSame(existing, removed);
        verify(fileRepository).remove(existing);
    }

    private static FileAggregate existingFile(Long uploaderId) {
        CreateFileParam param = new CreateFileParam();
        param.setOriginalName("a.txt");
        param.setContentType("text/plain");
        param.setFileSize(3L);
        param.setUploaderId(uploaderId);
        FileAggregate aggregate = FileAggregate.create(param);
        aggregate.getFile().setId(10L);
        return aggregate;
    }

    private static RemoveFileParam removeParam(Long fileId, Long operatorId) {
        RemoveFileParam param = new RemoveFileParam();
        param.setFileId(fileId);
        param.setOperatorId(operatorId);
        return param;
    }
}
