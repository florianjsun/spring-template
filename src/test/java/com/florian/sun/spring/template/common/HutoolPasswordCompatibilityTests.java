package com.florian.sun.spring.template.common;

import cn.hutool.crypto.digest.DigestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HutoolPasswordCompatibilityTests {

    @ParameterizedTest
    @ValueSource(strings = {"test-password", "\u5bc6\u7801-test"})
    void createsSaltedHashesAndChecksPasswords(String rawPassword) {
        String encodedPassword = DigestUtil.bcrypt(rawPassword);

        assertTrue(encodedPassword.startsWith("$2a$10$"));
        assertNotEquals(encodedPassword, DigestUtil.bcrypt(rawPassword));
        assertTrue(DigestUtil.bcryptCheck(rawPassword, encodedPassword));
        assertFalse(DigestUtil.bcryptCheck("wrong-password", encodedPassword));
    }

    @ParameterizedTest
    @MethodSource("legacyHashes")
    void matchesHashesCreatedBySaToken(String rawPassword, String encodedPassword) {
        assertTrue(DigestUtil.bcryptCheck(rawPassword, encodedPassword));
        assertFalse(DigestUtil.bcryptCheck("wrong-password", encodedPassword));
    }

    private static Stream<Arguments> legacyHashes() {
        // Generated with Sa-Token 1.46.0 BCrypt and a fixed salt for migration coverage.
        return Stream.of(
                Arguments.of("test-password", "$2a$10$N9qo8uLOickgx2ZMRZoMye/5zjtStVwh/EmbYQk3k.Hk3W2Uc44Ju"),
                Arguments.of("\u5bc6\u7801-test", "$2a$10$N9qo8uLOickgx2ZMRZoMyeSfl7/NOeieSA32M6lb9rLPTEi6shOf."),
                Arguments.of("a".repeat(100), "$2a$10$N9qo8uLOickgx2ZMRZoMye5mlC/WoAmNnGP3YkHGchsBkco85S4ZC")
        );
    }
}
