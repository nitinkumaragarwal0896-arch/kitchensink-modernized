package com.modernizedkitechensink.kitchensinkmodernized.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration.
 *
 * Provides interactive API documentation at /swagger-ui.html
 * Includes JWT authentication support for testing protected endpoints.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
      // API Information
      .info(new Info()
        .title("Kitchensink Modernized API")
        .version("1.0.0")
        .description("""
                                Modernized Kitchensink application - migrated from JBoss EAP to Spring Boot.
                                
                                ## Features
                                - Member Registration System
                                - JWT Authentication
                                - Role-Based Access Control (RBAC)
                                - Audit Logging
                                - Rate Limiting
                                
                                ## Authentication
                                1. Call POST /api/v1/auth/login with username/password
                                2. Copy the accessToken from response
                                3. Click 'Authorize' button above
                                4. Enter: Bearer {your-token}
                                
                                ## Default Users
                                - admin / admin123 (full access)
                                - user / user123 (limited access)
                                """)
        .contact(new Contact()
          .name("Developer")
          .email("developer@example.com"))
        .license(new License()
          .name("Apache 2.0")
          .url("https://www.apache.org/licenses/LICENSE-2.0")))

      // Server URLs
      .servers(List.of(
        new Server().url("http://localhost:8081").description("Local Development")
      ))

      // Security Configuration for JWT
      .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
      .components(new Components()
        .addSecuritySchemes(securitySchemeName,
          new SecurityScheme()
            .name(securitySchemeName)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Enter JWT token")
        ));
  }
}