package com.florian.sun.spring.template.infrastructure.user.repository;

import cn.hutool.core.util.StrUtil;
import com.florian.sun.spring.template.common.exception.BizAssert;
import com.florian.sun.spring.template.common.exception.CommonErrorCode;
import com.florian.sun.spring.template.common.result.PageResult;
import com.florian.sun.spring.template.domain.user.model.aggregate.UserAggregate;
import com.florian.sun.spring.template.domain.user.model.entity.UserEntity;
import com.florian.sun.spring.template.domain.user.model.param.PageUserQuery;
import com.florian.sun.spring.template.domain.user.repository.UserRepository;
import com.florian.sun.spring.template.infrastructure.user.converter.UserConverter;
import com.florian.sun.spring.template.infrastructure.user.mysql.mapper.UserMapper;
import com.florian.sun.spring.template.infrastructure.user.mysql.po.UserPO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.florian.sun.spring.template.infrastructure.user.mysql.po.table.UserTableDef.USER;

/**
 * 用户仓储实现
 *
 * @author Florian Sun
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    @Override
    public void save(UserAggregate aggregate) {
        UserEntity user = aggregate.getUser();
        UserPO po = userConverter.toPO(user);
        if (aggregate.isNew()) {
            userMapper.insertSelective(po);
            user.setId(po.getId());
            user.setVersion(0);
            return;
        }
        int rows = userMapper.update(po);
        BizAssert.isTrue(rows == 1, CommonErrorCode.CONCURRENT_CONFLICT);
        user.setVersion(user.getVersion() + 1);
    }

    @Override
    public Optional<UserAggregate> findById(Long id) {
        return Optional.ofNullable(userMapper.selectOneById(id))
                .map(userConverter::toAggregate);
    }

    @Override
    public Optional<UserAggregate> findByEmail(String email) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(USER.EMAIL.eq(email));
        return Optional.ofNullable(userMapper.selectOneByQuery(wrapper))
                .map(userConverter::toAggregate);
    }

    @Override
    public boolean existsByEmail(String email) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(USER.EMAIL.eq(email));
        return userMapper.selectCountByQuery(wrapper) > 0;
    }

    @Override
    public PageResult<UserAggregate> pageUsers(PageUserQuery query) {
        String keyword = query.getKeyword();
        // 关键字用括号分组：... AND (email LIKE ? OR nickname LIKE ?)，keyword 为空时整组忽略
        QueryWrapper wrapper = QueryWrapper.create()
                .where(USER.STATUS.eq(query.getStatus()))
                .and(USER.ROLE.eq(query.getRole()))
                .and(w -> {
                    w.where(USER.EMAIL.like(keyword, StrUtil::isNotBlank))
                            .or(USER.NICKNAME.like(keyword, StrUtil::isNotBlank));
                })
                .orderBy(USER.ID.desc());
        Page<UserPO> page = userMapper.paginate(Page.of(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getPageNumber(), page.getPageSize(), page.getTotalRow(),
                userConverter.toAggregateList(page.getRecords()));
    }
}
