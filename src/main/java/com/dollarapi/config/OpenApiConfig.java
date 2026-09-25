package com.dollarapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dollarApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dollar API Argentina - Resilient Java 21 Microservice")
                        .description("High-performance microservice providing live Argentine Dollar exchange rates (Oficial, Blue, MEP, CCL, Tarjeta, Cripto). "
                                + "Features Resilience4j Circuit Breaker, Automatic Exponential Retry, Redis Caching with In-Memory Fallback, and Chaos Testing Engine.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Engineering Team")
                                .url("https://github.com/dollar-api-java"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
