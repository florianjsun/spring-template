package com.florian.sun.spring.template.domain.user.model.aggregate;

import com.florian.sun.spring.template.common.model.BaseAggregate;
import com.florian.sun.spring.template.common.model.BaseEntity;
import com.florian.sun.spring.template.domain.user.model.entity.UserEntity;
import com.florian.sun.spring.template.domain.user.model.param.ChangePasswordParam;
import com.florian.sun.spring.template.domain.user.model.param.DisableUserParam;
import com.florian.sun.spring.template.domain.user.model.param.RegisterUserParam;
import com.florian.sun.spring.template.domain.user.model.param.UpdateProfileParam;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户聚合根
 * 单表聚合：只有根实体 UserEntity，方法全部委托
 *
 * @author Florian Sun
 */
@Getter
@Setter
public class UserAggregate extends BaseAggregate {

    /**
     * 根实体：t_user
     */
    private UserEntity user;

    @Override
    protected BaseEntity rootEntity() {
        return user;
    }

    public static UserAggregate create(RegisterUserParam param) {
        UserAggregate aggregate = new UserAggregate();
        aggregate.user = UserEntity.create(param);
        return aggregate;
    }

    public void verifyPassword(String rawPassword) {
        user.verifyPassword(rawPassword);
    }

    public void ensureActive() {
        user.ensureActive();
    }

    public void updateProfile(UpdateProfileParam param) {
        user.updateProfile(param);
    }

    public void changePassword(ChangePasswordParam param) {
        user.changePassword(param);
    }

    public void disable(DisableUserParam param) {
        user.disable(param);
    }

    public void enable() {
        user.enable();
    }

    public boolean isAdmin() {
        return user.isAdmin();
    }
}
