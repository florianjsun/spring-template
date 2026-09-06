package com.florian.sun.spring.template.application.file.assembler;

import com.florian.sun.spring.template.application.file.dto.req.PageMyFilesRequestDTO;
import com.florian.sun.spring.template.application.file.dto.req.UploadFileRequestDTO;
import com.florian.sun.spring.template.application.file.dto.res.FileContentResponseDTO;
import com.florian.sun.spring.template.application.file.dto.res.FileInfoResponseDTO;
import com.florian.sun.spring.template.domain.file.model.aggregate.FileAggregate;
import com.florian.sun.spring.template.domain.file.model.entity.FileEntity;
import com.florian.sun.spring.template.domain.file.model.param.CreateFileParam;
import com.florian.sun.spring.template.domain.file.model.param.PageFileQuery;
import com.florian.sun.spring.template.domain.file.model.param.RemoveFileParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.io.InputStream;

/**
 * 文件场景 DTO <-> 领域对象转换
 * 根实体属性通过 target = "." 平铺到 ResponseDTO
 *
 * @author Florian Sun
 */
@Mapper(componentModel = "spring")
public interface FileAssembler {

    @Mapping(target = "uploaderId", source = "operatorId")
    CreateFileParam toCreateFileParam(UploadFileRequestDTO dto);

    @Mapping(target = "fileId", source = "fileId")
    @Mapping(target = "operatorId", source = "operatorId")
    RemoveFileParam toRemoveFileParam(Long fileId, Long operatorId);

    @Mapping(target = "uploaderId", source = "operatorId")
    PageFileQuery toPageFileQuery(PageMyFilesRequestDTO dto);

    @Mapping(target = "fileId", source = "id")
    @Mapping(target = ".", source = "file")
    FileInfoResponseDTO toFileInfoResponseDTO(FileAggregate aggregate);

    default FileContentResponseDTO toFileContentResponseDTO(FileAggregate aggregate, InputStream content) {
        FileEntity file = aggregate.getFile();
        return new FileContentResponseDTO(file.getOriginalName(), file.getContentType(), file.getFileSize(), content);
    }
}
