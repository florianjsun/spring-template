package com.florian.sun.spring.template.common.result;

import com.florian.sun.spring.template.common.exception.CommonErrorCode;
import com.florian.sun.spring.template.common.exception.ErrorCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 统一接口返回结构
 * 仅在 adaptor 层构造，application / domain 禁止使用
 *
 * @author Florian Sun
 */
@Getter
@ToString
public class Result<T> {

    private final String code;
    private final String message;
    private final T data;

    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMessage(), data);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return CommonErrorCode.SUCCESS.getCode().equals(code);
    }
}
