package com.florian.sun.spring.template.domain.user.model.entity;

import com.florian.sun.spring.template.common.exception.BizAssert;
import com.florian.sun.spring.template.common.model.BaseEntity;
import com.florian.sun.spring.template.domain.user.model.enums.UserErrorCode;
import com.florian.sun.spring.template.domain.user.model.enums.UserRoleEnum;
import com.florian.sun.spring.template.domain.user.model.enums.UserStatusEnum;
import com.florian.sun.spring.template.domain.user.model.param.ChangePasswordParam;
import com.florian.sun.spring.template.domain.user.model.param.DisableUserParam;
import com.florian.sun.spring.template.domain.user.model.param.RegisterUserParam;
import com.florian.sun.spring.template.domain.user.model.param.UpdateProfileParam;
import com.florian.sun.spring.template.domain.user.model.value.PasswordValue;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 用户根实体
 * 与 t_user 一一对应，持有用户属性与单实体规则
 *
 * @author Florian Sun
 */
@Getter
@Setter
public class UserEntity extends BaseEntity {

    private String email;
    private PasswordValue password;
    private String nickname;
    private Long avatarFileId;
    private UserRoleEnum role;
    private UserStatusEnum status;

    /**
     * 注册：普通用户、正常状态、密码哈希
     */
    public static UserEntity create(RegisterUserParam param) {
        UserEntity user = new UserEntity();
        user.email = param.getEmail();
        user.password = PasswordValue.encode(param.getRawPassword());
        user.nickname = param.getNickname();
        user.role = UserRoleEnum.USER;
        user.status = UserStatusEnum.ACTIVE;
        return user;
    }

    public void verifyPassword(String rawPassword) {
        BizAssert.isTrue(password.matches(rawPassword), UserErrorCode.EMAIL_OR_PASSWORD_INCORRECT);
    }

    public void ensureActive() {
        BizAssert.isTrue(status == UserStatusEnum.ACTIVE, UserErrorCode.USER_DISABLED);
    }

    public void updateProfile(UpdateProfileParam param) {
        this.nickname = param.getNickname();
        this.avatarFileId = param.getAvatarFileId();
    }

    public void changePassword(ChangePasswordParam param) {
        BizAssert.isTrue(password.matches(param.getOldRawPassword()), UserErrorCode.OLD_PASSWORD_INCORRECT);
        BizAssert.isTrue(!password.matches(param.getNewRawPassword()), UserErrorCode.PASSWORD_SAME_AS_OLD);
        this.password = PasswordValue.encode(param.getNewRawPassword());
    }

    public void disable(DisableUserParam param) {
        BizAssert.isTrue(!Objects.equals(getId(), param.getOperatorId()), UserErrorCode.CANNOT_OPERATE_SELF);
        BizAssert.isTrue(status == UserStatusEnum.ACTIVE, UserErrorCode.USER_STATUS_INVALID);
        this.status = UserStatusEnum.DISABLED;
    }

    public void enable() {
        BizAssert.isTrue(status == UserStatusEnum.DISABLED, UserErrorCode.USER_STATUS_INVALID);
        this.status = UserStatusEnum.ACTIVE;
    }

    public boolean isAdmin() {
        return role == UserRoleEnum.ADMIN;
    }
}
