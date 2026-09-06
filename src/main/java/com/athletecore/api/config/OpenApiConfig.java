package com.athletecore.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configuración de la documentación OpenAPI (springdoc) de AthleteCore API.
 *
 * <p>Declara la metadata general de la API y el esquema de seguridad Bearer JWT,
 * de modo que el frontend (y Swagger UI) sepan cómo autenticar cada petición:
 * header {@code Authorization: Bearer <token>}.</p>
 *
 * <p>Nota: {@code spring.web.resources.add-mappings=false} en application.properties
 * desactiva los resource handlers por defecto, lo que rompería los assets estáticos
 * de Swagger UI (webjars). Por eso se registra explícitamente el handler de webjars.</p>
 *
 * <p>Los endpoints de documentación ({@code /v3/api-docs}, {@code /swagger-ui/**})
 * quedan protegidos por JWT como cualquier otro endpoint de esta API privada
 * (ver {@link SecurityConfig}).</p>
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    /** Nombre del esquema de seguridad referenciado en el contrato y por Swagger UI. */
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Definición global del documento OpenAPI: información de la API y
     * requisito de seguridad Bearer JWT aplicado a todos los endpoints.
     * @return configuración OpenAPI de AthleteCore API
     */
    @Bean
    public OpenAPI athleteCoreOpenAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("Token JWT obtenido en POST /api/v1/auth/login. "
                        + "Formato: Bearer <token>");

        return new OpenAPI()
                .info(new Info()
                        .title("AthleteCore API")
                        .version("0.0.1-SNAPSHOT")
                        .description("API REST para la gestión de rendimiento de deportistas: "
                                + "perfiles, ciclos de entrenamiento, asistencia, chequeos mensuales "
                                + "con proyección de medallería y reportes. "
                                + "Autenticación stateless mediante JWT (Bearer)."))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /**
     * Registra el handler de recursos estáticos necesario para servir los assets
     * de Swagger UI (webjars), deshabilitados por {@code add-mappings=false}.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
