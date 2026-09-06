package com.florian.sun.spring.template.domain.user.service;

import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.domain.user.model.aggregate.UserAggregate;
import com.florian.sun.spring.template.domain.user.model.enums.UserErrorCode;
import com.florian.sun.spring.template.domain.user.model.enums.UserStatusEnum;
import com.florian.sun.spring.template.domain.user.model.param.AuthenticateParam;
import com.florian.sun.spring.template.domain.user.model.param.DisableUserParam;
import com.florian.sun.spring.template.domain.user.model.param.RegisterUserParam;
import com.florian.sun.spring.template.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDomainServiceTests {

    private static final String EMAIL = "alice@example.com";
    private static final String RAW_PASSWORD = "Passw0rd!";

    private UserRepository userRepository;
    private UserDomainService userDomainService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDomainService = new UserDomainService(userRepository);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        BizException exception = assertThrows(BizException.class, () -> userDomainService.register(registerParam()));

        assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerSavesNewUser() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

        UserAggregate user = userDomainService.register(registerParam());

        assertEquals(EMAIL, user.getUser().getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void authenticateHidesWhetherEmailExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class,
                () -> userDomainService.authenticate(authenticateParam(RAW_PASSWORD)));

        assertEquals(UserErrorCode.EMAIL_OR_PASSWORD_INCORRECT.getCode(), exception.getCode());
    }

    @Test
    void authenticateRejectsWrongPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));

        BizException exception = assertThrows(BizException.class,
                () -> userDomainService.authenticate(authenticateParam("wrong")));

        assertEquals(UserErrorCode.EMAIL_OR_PASSWORD_INCORRECT.getCode(), exception.getCode());
    }

    @Test
    void authenticateRejectsDisabledUser() {
        UserAggregate disabled = existingUser();
        disabled.getUser().setId(1L);
        DisableUserParam param = new DisableUserParam();
        param.setUserId(1L);
        param.setOperatorId(99L);
        disabled.disable(param);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(disabled));

        BizException exception = assertThrows(BizException.class,
                () -> userDomainService.authenticate(authenticateParam(RAW_PASSWORD)));

        assertEquals(UserErrorCode.USER_DISABLED.getCode(), exception.getCode());
    }

    @Test
    void authenticateReturnsAggregateOnSuccess() {
        UserAggregate existing = existingUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        UserAggregate authenticated = userDomainService.authenticate(authenticateParam(RAW_PASSWORD));

        assertSame(existing, authenticated);
    }

    @Test
    void disableUserLoadsMutatesAndSaves() {
        UserAggregate existing = existingUser();
        existing.getUser().setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        DisableUserParam param = new DisableUserParam();
        param.setUserId(1L);
        param.setOperatorId(99L);

        userDomainService.disableUser(param);

        assertEquals(UserStatusEnum.DISABLED, existing.getUser().getStatus());
        verify(userRepository).save(existing);
    }

    @Test
    void enableUserRejectsUnknownUser() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class, () -> userDomainService.enableUser(404L));

        assertEquals(UserErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    private static RegisterUserParam registerParam() {
        RegisterUserParam param = new RegisterUserParam();
        param.setEmail(EMAIL);
        param.setRawPassword(RAW_PASSWORD);
        param.setNickname("Alice");
        return param;
    }

    private static AuthenticateParam authenticateParam(String rawPassword) {
        AuthenticateParam param = new AuthenticateParam();
        param.setEmail(EMAIL);
        param.setRawPassword(rawPassword);
        return param;
    }

    private static UserAggregate existingUser() {
        return UserAggregate.create(registerParam());
    }
}
