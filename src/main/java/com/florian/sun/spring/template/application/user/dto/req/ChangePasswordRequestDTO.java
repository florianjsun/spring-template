package com.florian.sun.spring.template.application.user.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/**
 * 修改密码请求
 *
 * @author Florian Sun
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequestDTO {

    @Schema(description = "原密码")
    @NotBlank(message = "原密码不能为空")
    @ToString.Exclude
    private String oldPassword;

    @Schema(description = "新密码")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度需在 8~64 之间")
    @ToString.Exclude
    private String newPassword;

    /** 操作人，由 Controller 从登录态注入 */
    @JsonIgnore
    @Schema(hidden = true)
    private Long operatorId;
}
