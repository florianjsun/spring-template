package com.florian.sun.spring.template.domain.user.repository;

import com.florian.sun.spring.template.common.result.PageResult;
import com.florian.sun.spring.template.domain.user.model.aggregate.UserAggregate;
import com.florian.sun.spring.template.domain.user.model.param.PageUserQuery;

import java.util.Optional;

/**
 * 用户仓储接口
 * 实现在 infrastructure 层 UserRepositoryImpl
 *
 * @author Florian Sun
 */
public interface UserRepository {

    /**
     * 保存聚合根（新增或更新），新增时回填根实体 id / version
     */
    void save(UserAggregate user);

    Optional<UserAggregate> findById(Long id);

    Optional<UserAggregate> findByEmail(String email);

    boolean existsByEmail(String email);

    PageResult<UserAggregate> pageUsers(PageUserQuery query);
}
