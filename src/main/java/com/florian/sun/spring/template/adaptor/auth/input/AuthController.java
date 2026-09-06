package com.florian.sun.spring.template.adaptor.auth.input;

import cn.dev33.satoken.annotation.SaIgnore;
import com.florian.sun.spring.template.application.auth.dto.req.LoginRequestDTO;
import com.florian.sun.spring.template.application.auth.dto.req.RegisterRequestDTO;
import com.florian.sun.spring.template.application.auth.dto.res.LoginResponseDTO;
import com.florian.sun.spring.template.application.auth.dto.res.RegisterResponseDTO;
import com.florian.sun.spring.template.application.auth.scenario.AuthAppService;
import com.florian.sun.spring.template.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 * register / login 已在 SaTokenConfig 放行，这里再加 @SaIgnore 明确意图
 *
 * @author Florian Sun
 */
@Tag(name = "认证", description = "注册 / 登录 / 注销")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;

    @Operation(summary = "注册")
    @SaIgnore
    @PostMapping("/register")
    public Result<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        return Result.success(authAppService.register(dto));
    }

    @Operation(summary = "登录")
    @SaIgnore
    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return Result.success(authAppService.login(dto));
    }

    @Operation(summary = "注销")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authAppService.logout();
        return Result.success();
    }
}
