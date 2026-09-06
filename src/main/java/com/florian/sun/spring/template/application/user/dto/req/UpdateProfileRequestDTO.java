package com.florian.sun.spring.template.application.user.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料请求
 *
 * @author Florian Sun
 */
@Data
@Schema(description = "修改个人资料请求")
public class UpdateProfileRequestDTO {

    @Schema(description = "昵称", example = "Alice")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称长度不能超过 32")
    private String nickname;

    @Schema(description = "头像文件ID，传空表示清除头像")
    @Positive(message = "头像文件ID必须为正数")
    private Long avatarFileId;

    /** 操作人，由 Controller 从登录态注入 */
    @JsonIgnore
    @Schema(hidden = true)
    private Long operatorId;
}
