package com.florian.sun.spring.template.common.exception;

/**
 * 错误码接口
 * 通用错误码在 CommonErrorCode，领域错误码在 domain/{业务名}/model/enums/{业务名}ErrorCode
 *
 * @author Florian Sun
 */
public interface ErrorCode {

    String getCode();

    String getMessage();
}
