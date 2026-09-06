package com.florian.sun.spring.template.common.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 聚合根基类
 * version 用于乐观锁，由 RepositoryImpl 从 PO 回填，业务代码禁止修改
 *
 * @author Florian Sun
 */
@Getter
@Setter
public abstract class BaseAggregate {

    /**
     * 主键，新建时为 null，save 后由 RepositoryImpl 回填
     */
    private Long id;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public boolean isNew() {
        return id == null;
    }
}
