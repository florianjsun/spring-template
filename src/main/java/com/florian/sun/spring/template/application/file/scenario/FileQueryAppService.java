package com.florian.sun.spring.template.application.file.scenario;

import com.florian.sun.spring.template.application.file.adaptor.FileStorageAdaptor;
import com.florian.sun.spring.template.application.file.assembler.FileAssembler;
import com.florian.sun.spring.template.application.file.dto.req.PageMyFilesRequestDTO;
import com.florian.sun.spring.template.application.file.dto.res.FileContentResponseDTO;
import com.florian.sun.spring.template.application.file.dto.res.FileInfoResponseDTO;
import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.common.result.PageResult;
import com.florian.sun.spring.template.domain.file.model.aggregate.FileAggregate;
import com.florian.sun.spring.template.domain.file.model.enums.FileErrorCode;
import com.florian.sun.spring.template.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 文件查询场景（读模式）
 * 登录用户可按 id 查看 / 下载任意文件（头像等场景）
 *
 * @author Florian Sun
 */
@Service
@RequiredArgsConstructor
public class FileQueryAppService {

    private final FileRepository fileRepository;
    private final FileStorageAdaptor fileStorageAdaptor;
    private final FileAssembler fileAssembler;

    public FileInfoResponseDTO getFileInfo(Long fileId) {
        return fileAssembler.toFileInfoResponseDTO(loadFile(fileId));
    }

    /**
     * 元数据存在但对象不存在时抛 FILE_CONTENT_NOT_FOUND，与 FILE_NOT_FOUND 区分
     */
    public FileContentResponseDTO downloadFile(Long fileId) {
        FileAggregate aggregate = loadFile(fileId);
        InputStream content = fileStorageAdaptor.load(aggregate.getFile().getStorageKey())
                .orElseThrow(() -> new BizException(FileErrorCode.FILE_CONTENT_NOT_FOUND));
        return fileAssembler.toFileContentResponseDTO(aggregate, content);
    }

    public PageResult<FileInfoResponseDTO> pageMyFiles(PageMyFilesRequestDTO dto) {
        return fileRepository.pageByUploader(fileAssembler.toPageFileQuery(dto))
                .map(fileAssembler::toFileInfoResponseDTO);
    }

    private FileAggregate loadFile(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BizException(FileErrorCode.FILE_NOT_FOUND));
    }
}
