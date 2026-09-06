package com.florian.sun.spring.template.domain.user.model.param;

import lombok.Data;

/**
 * 修改密码参数
 *
 * @author Florian Sun
 */
@Data
public class ChangePasswordParam {

    private Long userId;
    private String oldRawPassword;
    private String newRawPassword;
}
