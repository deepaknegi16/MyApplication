package com.hackerrank.hotel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hotelServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hotel Service API")
                        .description("Retrieve hotels, soft-delete them, and search a city's hotels "
                                + "sorted by haversine distance from the city center. "
                                + "Log in via POST /auth/login to get a JWT, then send it as "
                                + "'Authorization: Bearer <token>'.")
                        .version("1.0.0")
                        .contact(new Contact().name("Deepak Negi").email("deepaknegi.1616@gmail.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
