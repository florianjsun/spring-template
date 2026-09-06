package com.florian.sun.spring.template.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用错误码
 * 只放与具体业务无关的错误；业务错误码定义在各领域的 {业务名}ErrorCode 中
 *
 * @author Florian Sun
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    SUCCESS("0", "成功"),
    PARAM_ERROR("PARAM_ERROR", "参数错误"),
    NOT_LOGIN("NOT_LOGIN", "未登录或登录已过期"),
    NO_PERMISSION("NO_PERMISSION", "无访问权限"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    CONCURRENT_CONFLICT("CONCURRENT_CONFLICT", "数据已被他人修改，请刷新后重试"),
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR", "外部服务调用失败"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常，请稍后重试"),
    ;

    private final String code;
    private final String message;
}
