package com.florian.sun.spring.template.domain.file.service;

import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.domain.file.model.aggregate.FileAggregate;
import com.florian.sun.spring.template.domain.file.model.enums.FileErrorCode;
import com.florian.sun.spring.template.domain.file.model.param.CreateFileParam;
import com.florian.sun.spring.template.domain.file.model.param.RemoveFileParam;
import com.florian.sun.spring.template.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 文件领域服务（写模式）
 * 只管元数据；文件内容的存取由 Application 通过 FileStorageAdaptor 完成
 *
 * @author Florian Sun
 */
@Service
@RequiredArgsConstructor
public class FileDomainService {

    private final FileRepository fileRepository;

    /**
     * 创建元数据并持久化，返回聚合根供上层取 storageKey 写入对象存储
     */
    public FileAggregate createFile(CreateFileParam param) {
        FileAggregate file = FileAggregate.create(param);
        fileRepository.save(file);
        return file;
    }

    /**
     * 删除：加载 → 归属校验 → 逻辑删除，返回聚合根供上层删除对象存储中的内容
     */
    public FileAggregate removeFile(RemoveFileParam param) {
        FileAggregate file = fileRepository.findById(param.getFileId())
                .orElseThrow(() -> new BizException(FileErrorCode.FILE_NOT_FOUND));
        file.ensureOwnedBy(param.getOperatorId());
        fileRepository.remove(file);
        return file;
    }
}
