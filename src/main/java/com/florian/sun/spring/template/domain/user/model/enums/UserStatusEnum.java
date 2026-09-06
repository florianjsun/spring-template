package com.florian.sun.spring.template.domain.user.model.enums;

import com.florian.sun.spring.template.common.enums.BaseEnum;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户状态
 *
 * @author Florian Sun
 */
@Getter
@RequiredArgsConstructor
public enum UserStatusEnum implements BaseEnum<Integer> {

    DISABLED(0, "禁用"),
    ACTIVE(1, "正常"),
    ;

    @EnumValue
    private final Integer code;
    private final String description;
}
