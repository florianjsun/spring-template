package com.florian.sun.spring.template.application.auth.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 注册响应
 *
 * @author Florian Sun
 */
@Data
@Schema(description = "注册响应")
public class RegisterResponseDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "昵称")
    private String nickname;
}
