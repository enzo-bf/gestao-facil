package com.gestaofacil.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI gestaoFacilOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestão Fácil API")
                        .description("API REST com sessão JWT em cookie HttpOnly e proteção CSRF.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("GF-AUTH")));
    }
}
