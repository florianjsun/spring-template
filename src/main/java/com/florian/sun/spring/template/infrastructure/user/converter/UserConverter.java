package com.florian.sun.spring.template.infrastructure.user.converter;

import com.florian.sun.spring.template.domain.user.model.aggregate.UserAggregate;
import com.florian.sun.spring.template.domain.user.model.entity.UserEntity;
import com.florian.sun.spring.template.domain.user.model.value.PasswordValue;
import com.florian.sun.spring.template.infrastructure.user.mysql.po.UserPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 用户 PO <-> 领域对象转换
 * 两级映射：PO <-> 根实体，再把根实体装进聚合根
 *
 * @author Florian Sun
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    @Mapping(target = "password", source = "passwordHash")
    UserEntity toEntity(UserPO po);

    @Mapping(target = "user", source = "po")
    UserAggregate toAggregate(UserPO po);

    List<UserAggregate> toAggregateList(List<UserPO> poList);

    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "deleted", ignore = true)
    UserPO toPO(UserEntity user);

    default PasswordValue toPasswordValue(String hash) {
        return hash == null ? null : new PasswordValue(hash);
    }

    default String toPasswordHash(PasswordValue password) {
        return password == null ? null : password.hash();
    }
}
