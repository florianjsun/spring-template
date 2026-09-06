package com.florian.sun.spring.template.domain.user.model.param;

import lombok.Data;

/**
 * 认证参数（邮箱 + 密码）
 *
 * @author Florian Sun
 */
@Data
public class AuthenticateParam {

    private String email;
    private String rawPassword;
}
