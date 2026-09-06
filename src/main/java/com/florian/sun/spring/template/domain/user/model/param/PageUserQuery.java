package com.florian.sun.spring.template.domain.user.model.param;

import com.florian.sun.spring.template.common.model.PageQuery;
import com.florian.sun.spring.template.domain.user.model.enums.UserRoleEnum;
import com.florian.sun.spring.template.domain.user.model.enums.UserStatusEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * 分页查询用户条件
 *
 * @author Florian Sun
 */
@Getter
@Setter
public class PageUserQuery extends PageQuery {

    /** 邮箱 / 昵称模糊匹配 */
    private String keyword;
    private UserStatusEnum status;
    private UserRoleEnum role;
}
