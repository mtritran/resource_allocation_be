package com.company.resourceallocation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resourceAllocationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resource Allocation System API")
                        .description("API Documentation for Resource Allocation System")
                        .version("1.0.0"));
    }
}
