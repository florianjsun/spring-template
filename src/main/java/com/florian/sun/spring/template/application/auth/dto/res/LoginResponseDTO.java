package com.florian.sun.spring.template.application.auth.dto.res;

import com.florian.sun.spring.template.domain.user.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应
 *
 * @author Florian Sun
 */
@Data
@Schema(description = "登录响应")
public class LoginResponseDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色")
    private UserRoleEnum role;

    @Schema(description = "token 请求头名称", example = "Authorization")
    private String tokenName;

    @Schema(description = "token 值")
    private String tokenValue;

    @Schema(description = "token 有效期（秒）")
    private long timeoutSeconds;
}
