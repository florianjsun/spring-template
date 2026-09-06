package com.florian.sun.spring.template.adaptor.user.input;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.florian.sun.spring.template.application.user.dto.req.ChangePasswordRequestDTO;
import com.florian.sun.spring.template.application.user.dto.req.PageUsersRequestDTO;
import com.florian.sun.spring.template.application.user.dto.req.UpdateProfileRequestDTO;
import com.florian.sun.spring.template.application.user.dto.res.UserItemResponseDTO;
import com.florian.sun.spring.template.application.user.dto.res.UserProfileResponseDTO;
import com.florian.sun.spring.template.application.user.scenario.UserAppService;
import com.florian.sun.spring.template.application.user.scenario.UserQueryAppService;
import com.florian.sun.spring.template.common.result.PageResult;
import com.florian.sun.spring.template.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 * /users/me/** 为当前用户自助操作；其余为管理端，需 ADMIN 角色
 *
 * @author Florian Sun
 */
@Tag(name = "用户", description = "个人资料 / 用户管理")
@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final UserAppService userAppService;
    private final UserQueryAppService userQueryAppService;

    @Operation(summary = "查看我的资料")
    @GetMapping("/me")
    public Result<UserProfileResponseDTO> getMyProfile() {
        return Result.success(userQueryAppService.getMyProfile(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "修改我的资料")
    @PutMapping("/me/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO dto) {
        dto.setOperatorId(StpUtil.getLoginIdAsLong());
        userAppService.updateProfile(dto);
        return Result.success();
    }

    @Operation(summary = "修改我的密码")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO dto) {
        dto.setOperatorId(StpUtil.getLoginIdAsLong());
        userAppService.changePassword(dto);
        return Result.success();
    }

    @Operation(summary = "分页查询用户（管理端）")
    @SaCheckRole(ROLE_ADMIN)
    @GetMapping
    public Result<PageResult<UserItemResponseDTO>> pageUsers(@Valid @ParameterObject PageUsersRequestDTO dto) {
        return Result.success(userQueryAppService.pageUsers(dto));
    }

    @Operation(summary = "查看用户详情（管理端）")
    @SaCheckRole(ROLE_ADMIN)
    @GetMapping("/{userId}")
    public Result<UserProfileResponseDTO> getUserDetail(@PathVariable @Min(1) Long userId) {
        return Result.success(userQueryAppService.getUserDetail(userId));
    }

    @Operation(summary = "禁用用户（管理端）")
    @SaCheckRole(ROLE_ADMIN)
    @PostMapping("/{userId}/disable")
    public Result<Void> disableUser(@PathVariable @Min(1) Long userId) {
        userAppService.disableUser(userId, StpUtil.getLoginIdAsLong());
        return Result.success();
    }

    @Operation(summary = "启用用户（管理端）")
    @SaCheckRole(ROLE_ADMIN)
    @PostMapping("/{userId}/enable")
    public Result<Void> enableUser(@PathVariable @Min(1) Long userId) {
        userAppService.enableUser(userId);
        return Result.success();
    }
}
