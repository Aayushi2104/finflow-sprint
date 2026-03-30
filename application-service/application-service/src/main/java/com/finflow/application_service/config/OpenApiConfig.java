package com.finflow.application_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI applicationServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("FinFlow Application Service API")
                .version("v1")
                .description("Loan application APIs"));
    }
}
