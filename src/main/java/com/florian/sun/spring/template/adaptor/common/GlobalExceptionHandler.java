package com.florian.sun.spring.template.adaptor.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.common.exception.CommonErrorCode;
import com.florian.sun.spring.template.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 * 唯一允许把异常转换成 Result 的地方；业务异常 warn 不打堆栈，系统异常 error 打堆栈
 *
 * @author Florian Sun
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 领域 / 应用 / 适配器抛出的业务异常
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常 code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * RequestBody 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(CommonErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * RequestParam / @PathVariable / @ModelAttribute 参数校验失败
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<Void> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(CommonErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 请求体不可读、缺参数、类型不匹配
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("请求参数错误: {}", e.getMessage());
        return Result.fail(CommonErrorCode.PARAM_ERROR);
    }

    /**
     * 上传文件超出 spring.servlet.multipart 限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("上传文件超出大小限制: {}", e.getMessage());
        return Result.fail(CommonErrorCode.PARAM_ERROR.getCode(), "上传文件超出大小限制");
    }

    /**
     * Sa-Token：未登录
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(CommonErrorCode.NOT_LOGIN);
    }

    /**
     * Sa-Token：无权限 / 无角色
     */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public Result<Void> handleNoPermission(SaTokenException e) {
        log.warn("鉴权失败: {}", e.getMessage());
        return Result.fail(CommonErrorCode.NO_PERMISSION);
    }

    /**
     * 兜底：未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return Result.fail(CommonErrorCode.SYSTEM_ERROR);
    }
}
