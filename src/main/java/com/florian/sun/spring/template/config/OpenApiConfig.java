package com.florian.sun.spring.template.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc 配置
 * 声明 Sa-Token 请求头，方便在 swagger-ui 中直接携带 token 调试
 *
 * @author Florian Sun
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "satoken";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("spring-template API").version("0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(SECURITY_SCHEME)));
    }
}
