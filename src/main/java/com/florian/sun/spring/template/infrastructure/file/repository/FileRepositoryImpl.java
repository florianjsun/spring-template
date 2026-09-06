package com.florian.sun.spring.template.infrastructure.file.repository;

import cn.hutool.core.util.StrUtil;
import com.florian.sun.spring.template.common.exception.BizAssert;
import com.florian.sun.spring.template.common.exception.CommonErrorCode;
import com.florian.sun.spring.template.common.result.PageResult;
import com.florian.sun.spring.template.domain.file.model.aggregate.FileAggregate;
import com.florian.sun.spring.template.domain.file.model.entity.FileEntity;
import com.florian.sun.spring.template.domain.file.model.param.PageFileQuery;
import com.florian.sun.spring.template.domain.file.repository.FileRepository;
import com.florian.sun.spring.template.infrastructure.file.converter.FileConverter;
import com.florian.sun.spring.template.infrastructure.file.mysql.mapper.FileMapper;
import com.florian.sun.spring.template.infrastructure.file.mysql.po.FilePO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.florian.sun.spring.template.infrastructure.file.mysql.po.table.FileTableDef.FILE;

/**
 * 文件仓储实现
 *
 * @author Florian Sun
 */
@Repository
@RequiredArgsConstructor
public class FileRepositoryImpl implements FileRepository {

    private final FileMapper fileMapper;
    private final FileConverter fileConverter;

    @Override
    public void save(FileAggregate aggregate) {
        FileEntity file = aggregate.getFile();
        FilePO po = fileConverter.toPO(file);
        if (aggregate.isNew()) {
            fileMapper.insertSelective(po);
            file.setId(po.getId());
            file.setVersion(0);
            return;
        }
        int rows = fileMapper.update(po);
        BizAssert.isTrue(rows == 1, CommonErrorCode.CONCURRENT_CONFLICT);
        file.setVersion(file.getVersion() + 1);
    }

    @Override
    public Optional<FileAggregate> findById(Long id) {
        return Optional.ofNullable(fileMapper.selectOneById(id))
                .map(fileConverter::toAggregate);
    }

    @Override
    public boolean existsById(Long id) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(FILE.ID.eq(id));
        return fileMapper.selectCountByQuery(wrapper) > 0;
    }

    @Override
    public PageResult<FileAggregate> pageByUploader(PageFileQuery query) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(FILE.UPLOADER_ID.eq(query.getUploaderId()))
                .and(FILE.ORIGINAL_NAME.like(query.getKeyword(), StrUtil::isNotBlank))
                .orderBy(FILE.ID.desc());
        Page<FilePO> page = fileMapper.paginate(Page.of(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getPageNumber(), page.getPageSize(), page.getTotalRow(),
                fileConverter.toAggregateList(page.getRecords()));
    }

    @Override
    public void remove(FileAggregate aggregate) {
        fileMapper.deleteById(aggregate.getId());
    }
}
