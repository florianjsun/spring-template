package com.florian.sun.spring.template.application.file.scenario;

import com.florian.sun.spring.template.application.file.adaptor.FileStorageAdaptor;
import com.florian.sun.spring.template.application.file.assembler.FileAssembler;
import com.florian.sun.spring.template.application.file.dto.req.UploadFileRequestDTO;
import com.florian.sun.spring.template.application.file.dto.res.FileInfoResponseDTO;
import com.florian.sun.spring.template.domain.file.model.aggregate.FileAggregate;
import com.florian.sun.spring.template.domain.file.model.entity.FileEntity;
import com.florian.sun.spring.template.domain.file.service.FileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件写场景：上传 / 删除
 * 元数据在事务内落库，对象存储写入失败抛 BizException 使元数据回滚
 *
 * @author Florian Sun
 */
@Service
@RequiredArgsConstructor
public class FileAppService {

    private final FileDomainService fileDomainService;
    private final FileStorageAdaptor fileStorageAdaptor;
    private final FileAssembler fileAssembler;

    @Transactional(rollbackFor = Exception.class)
    public FileInfoResponseDTO uploadFile(UploadFileRequestDTO dto) {
        FileAggregate aggregate = fileDomainService.createFile(fileAssembler.toCreateFileParam(dto));
        FileEntity file = aggregate.getFile();
        fileStorageAdaptor.store(file.getStorageKey(), dto.getContent(), file.getFileSize(), file.getContentType());
        return fileAssembler.toFileInfoResponseDTO(aggregate);
    }

    /**
     * 先逻辑删除元数据，再删除对象；对象删除失败抛 BizException 使元数据回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId, Long operatorId) {
        FileAggregate aggregate = fileDomainService.removeFile(fileAssembler.toRemoveFileParam(fileId, operatorId));
        fileStorageAdaptor.delete(aggregate.getFile().getStorageKey());
    }
}
