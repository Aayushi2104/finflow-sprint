package com.finflow.admin_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI adminServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("FinFlow Admin Service API")
                .version("v1")
                .description("Administrative review and reporting APIs"));
    }
}
