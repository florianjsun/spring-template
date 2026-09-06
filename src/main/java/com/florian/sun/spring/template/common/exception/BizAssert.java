package com.florian.sun.spring.template.common.exception;

import cn.hutool.core.lang.Assert;

import java.util.Collection;

/**
 * 业务断言
 *
 * @author Florian Sun
 */
public final class BizAssert {

    private BizAssert() {
    }

    public static void isTrue(boolean condition, ErrorCode errorCode) {
        Assert.isTrue(condition, () -> new BizException(errorCode));
    }

    public static void isTrue(boolean condition, ErrorCode errorCode, String message) {
        Assert.isTrue(condition, () -> new BizException(errorCode, message));
    }

    public static void notNull(Object obj, ErrorCode errorCode) {
        Assert.notNull(obj, () -> new BizException(errorCode));
    }

    public static void notEmpty(Collection<?> collection, ErrorCode errorCode) {
        Assert.notEmpty(collection, () -> new BizException(errorCode));
    }

    public static void notBlank(String text, ErrorCode errorCode) {
        Assert.notBlank(text, () -> new BizException(errorCode));
    }
}
