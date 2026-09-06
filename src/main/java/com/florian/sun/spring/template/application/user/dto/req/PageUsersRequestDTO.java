package com.florian.sun.spring.template.application.user.dto.req;

import com.florian.sun.spring.template.common.model.PageQuery;
import com.florian.sun.spring.template.domain.user.model.enums.UserRoleEnum;
import com.florian.sun.spring.template.domain.user.model.enums.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 分页查询用户请求（管理端）
 *
 * @author Florian Sun
 */
@Getter
@Setter
@Schema(description = "分页查询用户请求")
public class PageUsersRequestDTO extends PageQuery {

    @Schema(description = "关键字，匹配邮箱或昵称")
    @Size(max = 64, message = "关键字长度不能超过 64")
    private String keyword;

    @Schema(description = "状态")
    private UserStatusEnum status;

    @Schema(description = "角色")
    private UserRoleEnum role;
}
