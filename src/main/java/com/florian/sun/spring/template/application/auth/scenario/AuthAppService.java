package com.florian.sun.spring.template.application.auth.scenario;

import com.florian.sun.spring.template.application.auth.adaptor.SessionAdaptor;
import com.florian.sun.spring.template.application.auth.adaptor.SessionDTO;
import com.florian.sun.spring.template.application.auth.assembler.AuthAssembler;
import com.florian.sun.spring.template.application.auth.dto.req.LoginRequestDTO;
import com.florian.sun.spring.template.application.auth.dto.req.RegisterRequestDTO;
import com.florian.sun.spring.template.application.auth.dto.res.LoginResponseDTO;
import com.florian.sun.spring.template.application.auth.dto.res.RegisterResponseDTO;
import com.florian.sun.spring.template.domain.user.model.aggregate.UserAggregate;
import com.florian.sun.spring.template.domain.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证场景：注册 / 登录 / 注销
 * 登录不落库，不加事务
 *
 * @author Florian Sun
 */
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final UserDomainService userDomainService;
    private final SessionAdaptor sessionAdaptor;
    private final AuthAssembler authAssembler;

    /**
     * 自助注册，注册后不自动登录
     */
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponseDTO register(RegisterRequestDTO dto) {
        UserAggregate user = userDomainService.register(authAssembler.toRegisterUserParam(dto));
        return authAssembler.toRegisterResponseDTO(user);
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        UserAggregate user = userDomainService.authenticate(authAssembler.toAuthenticateParam(dto));
        SessionDTO session = sessionAdaptor.login(user.getId());
        return authAssembler.toLoginResponseDTO(user, session);
    }

    public void logout() {
        sessionAdaptor.logout();
    }
}
