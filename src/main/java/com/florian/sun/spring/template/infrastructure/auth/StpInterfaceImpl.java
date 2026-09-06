package com.florian.sun.spring.template.infrastructure.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.florian.sun.spring.template.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限数据源
 * 角色码取自 UserRoleEnum.name()，供 @SaCheckRole 使用；本模板不使用细粒度权限码
 *
 * @author Florian Sun
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserRepository userRepository;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return userRepository.findById(Long.valueOf(loginId.toString()))
                .map(user -> List.of(user.getUser().getRole().name()))
                .orElse(List.of());
    }
}
