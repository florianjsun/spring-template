package com.florian.sun.spring.template.domain.user.service;

import com.florian.sun.spring.template.common.exception.BizAssert;
import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.domain.user.model.aggregate.UserAggregate;
import com.florian.sun.spring.template.domain.user.model.enums.UserErrorCode;
import com.florian.sun.spring.template.domain.user.model.param.AuthenticateParam;
import com.florian.sun.spring.template.domain.user.model.param.ChangePasswordParam;
import com.florian.sun.spring.template.domain.user.model.param.DisableUserParam;
import com.florian.sun.spring.template.domain.user.model.param.RegisterUserParam;
import com.florian.sun.spring.template.domain.user.model.param.UpdateProfileParam;
import com.florian.sun.spring.template.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户领域服务（写模式）
 * 稳定的领域能力，被认证、用户管理等多个场景复用
 *
 * @author Florian Sun
 */
@Service
@RequiredArgsConstructor
public class UserDomainService {

    private final UserRepository userRepository;

    /**
     * 注册：邮箱唯一 → 工厂方法建聚合根 → 持久化
     */
    public UserAggregate register(RegisterUserParam param) {
        BizAssert.isTrue(!userRepository.existsByEmail(param.getEmail()), UserErrorCode.EMAIL_ALREADY_EXISTS);
        UserAggregate user = UserAggregate.create(param);
        userRepository.save(user);
        return user;
    }

    /**
     * 认证：邮箱 + 密码 + 账号状态，全部通过返回聚合根，否则抛 BizException
     * 用户不存在与密码错误返回同一个错误码，避免泄漏账号是否存在
     */
    public UserAggregate authenticate(AuthenticateParam param) {
        UserAggregate user = userRepository.findByEmail(param.getEmail())
                .orElseThrow(() -> new BizException(UserErrorCode.EMAIL_OR_PASSWORD_INCORRECT));
        user.verifyPassword(param.getRawPassword());
        user.ensureActive();
        return user;
    }

    public void updateProfile(UpdateProfileParam param) {
        UserAggregate user = loadUser(param.getUserId());
        user.updateProfile(param);
        userRepository.save(user);
    }

    public void changePassword(ChangePasswordParam param) {
        UserAggregate user = loadUser(param.getUserId());
        user.changePassword(param);
        userRepository.save(user);
    }

    public void disableUser(DisableUserParam param) {
        UserAggregate user = loadUser(param.getUserId());
        user.disable(param);
        userRepository.save(user);
    }

    public void enableUser(Long userId) {
        UserAggregate user = loadUser(userId);
        user.enable();
        userRepository.save(user);
    }

    private UserAggregate loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
    }
}
