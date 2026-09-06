package com.florian.sun.spring.template.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * 领域层 / 应用层校验失败时抛出，由 GlobalExceptionHandler 统一转换为 Result
 *
 * @author Florian Sun
 */
@Getter
public class BizException extends RuntimeException {

    private final String code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /** 覆盖默认提示，如拼接具体的业务参数 */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /** 包装底层异常，保留 cause 便于排查 */
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }
}
