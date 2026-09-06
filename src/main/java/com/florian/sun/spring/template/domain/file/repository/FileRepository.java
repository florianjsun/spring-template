package com.florian.sun.spring.template.domain.file.repository;

import com.florian.sun.spring.template.common.result.PageResult;
import com.florian.sun.spring.template.domain.file.model.aggregate.FileAggregate;
import com.florian.sun.spring.template.domain.file.model.param.PageFileQuery;

import java.util.Optional;

/**
 * 文件仓储接口
 * 实现在 infrastructure 层 FileRepositoryImpl
 *
 * @author Florian Sun
 */
public interface FileRepository {

    /**
     * 保存聚合根（新增或更新），新增时回填根实体 id / version
     */
    void save(FileAggregate file);

    Optional<FileAggregate> findById(Long id);

    boolean existsById(Long id);

    PageResult<FileAggregate> pageByUploader(PageFileQuery query);

    /**
     * 逻辑删除元数据
     */
    void remove(FileAggregate file);
}
