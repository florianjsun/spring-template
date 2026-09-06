package com.florian.sun.spring.template.application.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

/**
 * 登录请求
 *
 * @author Florian Sun
 */
@Data
@Schema(description = "登录请求")
public class LoginRequestDTO {

    @Schema(description = "邮箱", example = "alice@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "密码", example = "Passw0rd!")
    @NotBlank(message = "密码不能为空")
    @ToString.Exclude
    private String password;
}
