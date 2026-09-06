package com.florian.sun.spring.template.common.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 实体基类
 * 实体只在聚合内部有意义，相等性由 id 决定
 *
 * @author Florian Sun
 */
@Getter
@Setter
public abstract class BaseEntity {

    private Long id;

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
