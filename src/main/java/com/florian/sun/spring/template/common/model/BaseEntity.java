package com.florian.sun.spring.template.common.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 实体基类
 * 实体只在聚合内部有意义，相等性由 id 决定；
 * id / version / createTime / updateTime 由 RepositoryImpl 从 PO 回填，业务代码禁止修改
 *
 * @author Florian Sun
 */
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * 主键，新建时为 null，save 后由 RepositoryImpl 回填
     */
    private Long id;

    /**
     * 乐观锁版本号，对应表的 version 列
     */
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
