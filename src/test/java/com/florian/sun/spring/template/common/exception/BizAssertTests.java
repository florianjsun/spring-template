package com.florian.sun.spring.template.common.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BizAssertTests {

    @Test
    void preservesBusinessErrorCodeAndDefaultMessage() {
        ErrorCode errorCode = CommonErrorCode.PARAM_ERROR;
        List<Executable> failures = List.of(
                () -> BizAssert.isTrue(false, errorCode),
                () -> BizAssert.notNull(null, errorCode),
                () -> BizAssert.notEmpty(null, errorCode),
                () -> BizAssert.notEmpty(List.of(), errorCode),
                () -> BizAssert.notBlank(null, errorCode),
                () -> BizAssert.notBlank("", errorCode),
                () -> BizAssert.notBlank(" \t\n", errorCode),
                () -> BizAssert.notBlank("\u00a0", errorCode)
        );

        for (Executable failure : failures) {
            BizException exception = assertThrows(BizException.class, failure);
            assertEquals(errorCode.getCode(), exception.getCode());
            assertEquals(errorCode.getMessage(), exception.getMessage());
        }
    }

    @Test
    void preservesCustomMessage() {
        BizException exception = assertThrows(BizException.class,
                () -> BizAssert.isTrue(false, CommonErrorCode.PARAM_ERROR, "Custom message"));

        assertEquals(CommonErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("Custom message", exception.getMessage());
    }

    @Test
    void successfulAssertionsDoNotConstructExceptions() {
        assertDoesNotThrow(() -> {
            BizAssert.isTrue(true, null);
            BizAssert.isTrue(true, null, "Custom message");
            BizAssert.notNull(new Object(), null);
            BizAssert.notEmpty(List.of("value"), null);
            BizAssert.notBlank(" value ", null);
        });
    }
}
