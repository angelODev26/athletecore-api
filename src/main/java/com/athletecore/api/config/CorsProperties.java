package com.athletecore.api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de CORS de la aplicación.
 *
 * <p>Estas propiedades ({@code spring.web.cors.*}) NO son estándar de Spring Boot:
 * se enlazan explícitamente a este POJO y se consumen en {@link CorsConfig}.</p>
 *
 * <p>En desarrollo local (perfil {@code local}) se definen con los orígenes del
 * frontend (Vite en localhost:5173). En producción deben quedar vacías salvo que
 * se configure la variable de entorno {@code CORS_ORIGINS}, lo que equivale a
 * CORS deshabilitado (ver {@link CorsConfig}).</p>
 *
 * @param allowedOrigins orígenes permitidos (vacío = CORS deshabilitado)
 * @param allowedMethods métodos HTTP permitidos
 * @param allowedHeaders headers permitidos
 * @param allowCredentials si se permiten credenciales (cookies/Authorization) en CORS
 */
@ConfigurationProperties(prefix = "spring.web.cors")
public record CorsProperties(List<String> allowedOrigins,
                             List<String> allowedMethods,
                             List<String> allowedHeaders,
                             Boolean allowCredentials) {
}
