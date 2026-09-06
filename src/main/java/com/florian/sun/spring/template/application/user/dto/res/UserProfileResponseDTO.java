package com.florian.sun.spring.template.application.user.dto.res;

import com.florian.sun.spring.template.domain.user.model.enums.UserRoleEnum;
import com.florian.sun.spring.template.domain.user.model.enums.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户详情
 *
 * @author Florian Sun
 */
@Data
@Schema(description = "用户详情")
public class UserProfileResponseDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像文件ID")
    private Long avatarFileId;

    @Schema(description = "角色")
    private UserRoleEnum role;

    @Schema(description = "状态")
    private UserStatusEnum status;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
