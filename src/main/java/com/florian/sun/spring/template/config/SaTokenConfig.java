package com.florian.sun.spring.template.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 * 注册全局登录拦截器；放行登录、文档、错误页
 *
 * @author Florian Sun
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private static final String[] EXCLUDE_PATHS = {
            "/auth/login",
            "/auth/register",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // SaInterceptor 同时开启注解鉴权（@SaCheckLogin / @SaCheckPermission / @SaCheckRole）
        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }
}
