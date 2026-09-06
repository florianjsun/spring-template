package com.florian.sun.spring.template.common.model;

/**
 * 聚合根基类
 * 聚合根是实体与值对象的容器，本身不持有任何属性；身份与乐观锁来自根实体
 *
 * @author Florian Sun
 */
public abstract class BaseAggregate {

    /**
     * 根实体：与主表一一对应，聚合的 id / version 来源
     */
    protected abstract BaseEntity rootEntity();

    /**
     * 聚合根 ID，即根实体 ID；新建时为 null，save 后由 RepositoryImpl 回填到根实体
     */
    public final Long getId() {
        return rootEntity().getId();
    }

    public final boolean isNew() {
        return getId() == null;
    }
}
