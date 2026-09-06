package com.florian.sun.spring.template.application.auth.adaptor;

/**
 * 会话适配器接口
 * 屏蔽 Sa-Token 细节，实现在 adaptor/auth/output/SaTokenSessionAdaptorImpl
 *
 * @author Florian Sun
 */
public interface SessionAdaptor {

    /**
     * 为用户创建登录会话
     */
    SessionDTO login(Long userId);

    /**
     * 注销当前会话
     */
    void logout();

    /**
     * 将指定用户的所有会话踢下线（禁用用户时使用）
     */
    void kickout(Long userId);
}
