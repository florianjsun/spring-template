package com.florian.sun.spring.template.infrastructure.user.mysql.po;

import com.florian.sun.spring.template.domain.user.model.enums.UserRoleEnum;
import com.florian.sun.spring.template.domain.user.model.enums.UserStatusEnum;
import com.florian.sun.spring.template.infrastructure.common.po.BasePO;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表 t_user
 *
 * @author Florian Sun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_user")
public class UserPO extends BasePO {

    private String email;
    /** BCrypt 哈希，领域侧为 PasswordValue */
    private String passwordHash;
    private String nickname;
    private Long avatarFileId;
    private UserRoleEnum role;
    private UserStatusEnum status;
}
