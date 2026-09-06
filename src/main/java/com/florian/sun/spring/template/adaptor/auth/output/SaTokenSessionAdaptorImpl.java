package com.florian.sun.spring.template.adaptor.auth.output;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.florian.sun.spring.template.application.auth.adaptor.SessionAdaptor;
import com.florian.sun.spring.template.application.auth.adaptor.SessionDTO;
import org.springframework.stereotype.Component;

/**
 * 会话适配器 Sa-Token 实现
 *
 * @author Florian Sun
 */
@Component
public class SaTokenSessionAdaptorImpl implements SessionAdaptor {

    @Override
    public SessionDTO login(Long userId) {
        StpUtil.login(userId);
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return new SessionDTO(tokenInfo.getTokenName(), tokenInfo.getTokenValue(), tokenInfo.getTokenTimeout());
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public void kickout(Long userId) {
        StpUtil.kickout(userId);
    }
}
