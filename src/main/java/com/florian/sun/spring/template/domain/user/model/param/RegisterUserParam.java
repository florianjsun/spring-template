package com.florian.sun.spring.template.domain.user.model.param;

import lombok.Data;

/**
 * 注册用户参数
 *
 * @author Florian Sun
 */
@Data
public class RegisterUserParam {

    private String email;
    private String rawPassword;
    private String nickname;
}
