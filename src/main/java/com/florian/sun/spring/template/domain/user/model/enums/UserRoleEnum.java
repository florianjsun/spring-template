package com.florian.sun.spring.template.domain.user.model.enums;

import com.florian.sun.spring.template.common.enums.BaseEnum;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户角色
 * 单字段角色模型；Sa-Token 角色码使用枚举 name()
 *
 * @author Florian Sun
 */
@Getter
@RequiredArgsConstructor
public enum UserRoleEnum implements BaseEnum<Integer> {

    USER(1, "普通用户"),
    ADMIN(2, "管理员"),
    ;

    @EnumValue
    private final Integer code;
    private final String description;
}
