package io.spring;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  static final String SECURITY_SCHEME_NAME = "Token";

  @Bean
  public OpenAPI conduitOpenApi(ObjectProvider<BuildProperties> buildProperties) {
    BuildProperties build = buildProperties.getIfAvailable();
    String version = build != null ? build.getVersion() : "unknown";
    return new OpenAPI()
        .info(
            new Info()
                .title("Conduit API")
                .version(version)
                .description(
                    "REST API for the Conduit social blogging platform (RealWorld spec). "
                        + "Log in via POST /users/login, then authorize with the header "
                        + "`Authorization: Token <jwt>`."))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("Token <jwt>")));
  }
}
