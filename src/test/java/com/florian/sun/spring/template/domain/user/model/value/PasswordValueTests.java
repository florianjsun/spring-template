package com.florian.sun.spring.template.domain.user.model.value;

import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.domain.user.model.enums.UserErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValueTests {

    @Test
    void encodeProducesBcryptHashThatMatchesRawPassword() {
        PasswordValue password = PasswordValue.encode("Passw0rd!");

        assertTrue(password.hash().startsWith("$2a$"));
        assertNotEquals("Passw0rd!", password.hash());
        assertTrue(password.matches("Passw0rd!"));
        assertFalse(password.matches("wrong"));
        assertFalse(password.matches(null));
    }

    @Test
    void encodeRejectsBlankPassword() {
        BizException exception = assertThrows(BizException.class, () -> PasswordValue.encode(" "));

        assertEquals(UserErrorCode.PASSWORD_INVALID.getCode(), exception.getCode());
    }

    @Test
    void encodeRejectsPasswordLongerThan72Bytes() {
        String tooLong = "a".repeat(73);
        String exactly72 = "a".repeat(72);

        BizException exception = assertThrows(BizException.class, () -> PasswordValue.encode(tooLong));
        assertEquals(UserErrorCode.PASSWORD_INVALID.getCode(), exception.getCode());
        assertTrue(PasswordValue.encode(exactly72).matches(exactly72));
    }

    @Test
    void encodeCountsUtf8BytesNotCharacters() {
        // 25 个汉字 = 75 字节，超过 72
        String chinese = "密".repeat(25);

        BizException exception = assertThrows(BizException.class, () -> PasswordValue.encode(chinese));
        assertEquals(UserErrorCode.PASSWORD_INVALID.getCode(), exception.getCode());
    }

    @Test
    void constructorRejectsBlankHash() {
        BizException exception = assertThrows(BizException.class, () -> new PasswordValue(""));

        assertEquals(UserErrorCode.PASSWORD_INVALID.getCode(), exception.getCode());
    }
}
