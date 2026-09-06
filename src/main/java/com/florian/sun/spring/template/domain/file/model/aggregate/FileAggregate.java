package com.florian.sun.spring.template.domain.file.model.aggregate;

import com.florian.sun.spring.template.common.model.BaseAggregate;
import com.florian.sun.spring.template.common.model.BaseEntity;
import com.florian.sun.spring.template.domain.file.model.entity.FileEntity;
import com.florian.sun.spring.template.domain.file.model.param.CreateFileParam;
import lombok.Getter;
import lombok.Setter;

/**
 * 文件聚合根
 * 单表聚合：只有根实体 FileEntity，方法全部委托
 *
 * @author Florian Sun
 */
@Getter
@Setter
public class FileAggregate extends BaseAggregate {

    /**
     * 根实体：t_file
     */
    private FileEntity file;

    @Override
    protected BaseEntity rootEntity() {
        return file;
    }

    public static FileAggregate create(CreateFileParam param) {
        FileAggregate aggregate = new FileAggregate();
        aggregate.file = FileEntity.create(param);
        return aggregate;
    }

    public void ensureOwnedBy(Long operatorId) {
        file.ensureOwnedBy(operatorId);
    }

    public boolean isOwnedBy(Long operatorId) {
        return file.isOwnedBy(operatorId);
    }
}
