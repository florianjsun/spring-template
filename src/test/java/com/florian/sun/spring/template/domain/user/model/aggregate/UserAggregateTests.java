package com.florian.sun.spring.template.domain.user.model.aggregate;

import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.domain.user.model.enums.UserErrorCode;
import com.florian.sun.spring.template.domain.user.model.enums.UserRoleEnum;
import com.florian.sun.spring.template.domain.user.model.enums.UserStatusEnum;
import com.florian.sun.spring.template.domain.user.model.param.ChangePasswordParam;
import com.florian.sun.spring.template.domain.user.model.param.DisableUserParam;
import com.florian.sun.spring.template.domain.user.model.param.RegisterUserParam;
import com.florian.sun.spring.template.domain.user.model.param.UpdateProfileParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAggregateTests {

    private static final String RAW_PASSWORD = "Passw0rd!";

    @Test
    void createBuildsActiveNormalUserWithHashedPassword() {
        UserAggregate aggregate = newUser();

        assertTrue(aggregate.isNew());
        assertNull(aggregate.getId());
        assertEquals("alice@example.com", aggregate.getUser().getEmail());
        assertEquals("Alice", aggregate.getUser().getNickname());
        assertEquals(UserRoleEnum.USER, aggregate.getUser().getRole());
        assertEquals(UserStatusEnum.ACTIVE, aggregate.getUser().getStatus());
        assertFalse(aggregate.isAdmin());
        assertNotEquals(RAW_PASSWORD, aggregate.getUser().getPassword().hash());
        assertDoesNotThrow(() -> aggregate.verifyPassword(RAW_PASSWORD));
    }

    @Test
    void idIsDelegatedToRootEntity() {
        UserAggregate aggregate = newUser();
        aggregate.getUser().setId(42L);

        assertEquals(42L, aggregate.getId());
        assertFalse(aggregate.isNew());
    }

    @Test
    void verifyPasswordRejectsWrongPassword() {
        UserAggregate aggregate = newUser();

        BizException exception = assertThrows(BizException.class, () -> aggregate.verifyPassword("wrong"));
        assertEquals(UserErrorCode.EMAIL_OR_PASSWORD_INCORRECT.getCode(), exception.getCode());
    }

    @Test
    void ensureActiveRejectsDisabledUser() {
        UserAggregate aggregate = newUser();
        aggregate.getUser().setId(1L);
        aggregate.disable(disableParam(1L, 99L));

        BizException exception = assertThrows(BizException.class, aggregate::ensureActive);
        assertEquals(UserErrorCode.USER_DISABLED.getCode(), exception.getCode());
    }

    @Test
    void updateProfileReplacesNicknameAndAvatar() {
        UserAggregate aggregate = newUser();
        UpdateProfileParam param = new UpdateProfileParam();
        param.setNickname("Alice Liddell");
        param.setAvatarFileId(7L);

        aggregate.updateProfile(param);

        assertEquals("Alice Liddell", aggregate.getUser().getNickname());
        assertEquals(7L, aggregate.getUser().getAvatarFileId());
    }

    @Test
    void changePasswordRequiresCorrectOldPassword() {
        UserAggregate aggregate = newUser();

        BizException exception = assertThrows(BizException.class,
                () -> aggregate.changePassword(changeParam("wrong", "NewPassw0rd!")));
        assertEquals(UserErrorCode.OLD_PASSWORD_INCORRECT.getCode(), exception.getCode());
        assertDoesNotThrow(() -> aggregate.verifyPassword(RAW_PASSWORD));
    }

    @Test
    void changePasswordRejectsSameAsOld() {
        UserAggregate aggregate = newUser();

        BizException exception = assertThrows(BizException.class,
                () -> aggregate.changePassword(changeParam(RAW_PASSWORD, RAW_PASSWORD)));
        assertEquals(UserErrorCode.PASSWORD_SAME_AS_OLD.getCode(), exception.getCode());
    }

    @Test
    void changePasswordReplacesHash() {
        UserAggregate aggregate = newUser();

        aggregate.changePassword(changeParam(RAW_PASSWORD, "NewPassw0rd!"));

        assertDoesNotThrow(() -> aggregate.verifyPassword("NewPassw0rd!"));
        assertThrows(BizException.class, () -> aggregate.verifyPassword(RAW_PASSWORD));
    }

    @Test
    void disableRejectsOperatingOnSelf() {
        UserAggregate aggregate = newUser();
        aggregate.getUser().setId(1L);

        BizException exception = assertThrows(BizException.class, () -> aggregate.disable(disableParam(1L, 1L)));
        assertEquals(UserErrorCode.CANNOT_OPERATE_SELF.getCode(), exception.getCode());
        assertEquals(UserStatusEnum.ACTIVE, aggregate.getUser().getStatus());
    }

    @Test
    void disableAndEnableFollowStatusTransitions() {
        UserAggregate aggregate = newUser();
        aggregate.getUser().setId(1L);

        BizException enableActive = assertThrows(BizException.class, aggregate::enable);
        assertEquals(UserErrorCode.USER_STATUS_INVALID.getCode(), enableActive.getCode());

        aggregate.disable(disableParam(1L, 99L));
        assertEquals(UserStatusEnum.DISABLED, aggregate.getUser().getStatus());

        BizException disableTwice = assertThrows(BizException.class, () -> aggregate.disable(disableParam(1L, 99L)));
        assertEquals(UserErrorCode.USER_STATUS_INVALID.getCode(), disableTwice.getCode());

        aggregate.enable();
        assertEquals(UserStatusEnum.ACTIVE, aggregate.getUser().getStatus());
    }

    private static UserAggregate newUser() {
        RegisterUserParam param = new RegisterUserParam();
        param.setEmail("alice@example.com");
        param.setRawPassword(RAW_PASSWORD);
        param.setNickname("Alice");
        return UserAggregate.create(param);
    }

    private static ChangePasswordParam changeParam(String oldRaw, String newRaw) {
        ChangePasswordParam param = new ChangePasswordParam();
        param.setOldRawPassword(oldRaw);
        param.setNewRawPassword(newRaw);
        return param;
    }

    private static DisableUserParam disableParam(Long userId, Long operatorId) {
        DisableUserParam param = new DisableUserParam();
        param.setUserId(userId);
        param.setOperatorId(operatorId);
        return param;
    }
}
