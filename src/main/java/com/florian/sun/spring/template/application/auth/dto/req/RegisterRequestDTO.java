package com.florian.sun.spring.template.application.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/**
 * 注册请求
 *
 * @author Florian Sun
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequestDTO {

    @Schema(description = "邮箱（登录账号）", example = "alice@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    @Schema(description = "密码", example = "Passw0rd!")
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需在 8~64 之间")
    @ToString.Exclude
    private String password;

    @Schema(description = "昵称", example = "Alice")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称长度不能超过 32")
    private String nickname;
}
