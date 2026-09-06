package com.florian.sun.spring.template.infrastructure.file.converter;

import com.florian.sun.spring.template.domain.file.model.aggregate.FileAggregate;
import com.florian.sun.spring.template.domain.file.model.entity.FileEntity;
import com.florian.sun.spring.template.infrastructure.file.mysql.po.FilePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 文件 PO <-> 领域对象转换
 * 两级映射：PO <-> 根实体，再把根实体装进聚合根
 *
 * @author Florian Sun
 */
@Mapper(componentModel = "spring")
public interface FileConverter {

    FileEntity toEntity(FilePO po);

    @Mapping(target = "file", source = "po")
    FileAggregate toAggregate(FilePO po);

    List<FileAggregate> toAggregateList(List<FilePO> poList);

    @Mapping(target = "deleted", ignore = true)
    FilePO toPO(FileEntity file);
}
