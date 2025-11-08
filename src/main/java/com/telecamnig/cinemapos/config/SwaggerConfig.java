package com.telecamnig.cinemapos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI cinemaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cinema POS Application")
                        .description("API documentation for Cinema Management - Desktop Application")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("support@demo.com")));
    }
}