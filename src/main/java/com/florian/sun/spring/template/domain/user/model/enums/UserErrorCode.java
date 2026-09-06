package com.florian.sun.spring.template.domain.user.model.enums;

import com.florian.sun.spring.template.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户领域错误码
 *
 * @author Florian Sun
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "邮箱已被注册"),
    /** 用户不存在与密码错误共用，避免泄漏账号是否存在 */
    EMAIL_OR_PASSWORD_INCORRECT("EMAIL_OR_PASSWORD_INCORRECT", "邮箱或密码错误"),
    USER_DISABLED("USER_DISABLED", "账号已被禁用"),
    PASSWORD_INVALID("PASSWORD_INVALID", "密码不能为空且 UTF-8 编码后不能超过 72 字节"),
    OLD_PASSWORD_INCORRECT("OLD_PASSWORD_INCORRECT", "原密码错误"),
    PASSWORD_SAME_AS_OLD("PASSWORD_SAME_AS_OLD", "新密码不能与原密码相同"),
    USER_STATUS_INVALID("USER_STATUS_INVALID", "当前用户状态不允许该操作"),
    CANNOT_OPERATE_SELF("CANNOT_OPERATE_SELF", "不能对自己执行该操作"),
    ;

    private final String code;
    private final String message;
}
