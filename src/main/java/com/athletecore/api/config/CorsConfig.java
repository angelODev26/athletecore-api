package com.athletecore.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de CORS para consumo de la API desde el frontend.
 *
 * <p>Política: en desarrollo local se habilita solo para el dev server del
 * frontend (por defecto localhost:5173 y localhost:3000, ver
 * application-local.properties). Si no hay orígenes configurados
 * (comportamiento por defecto en producción), CORS queda deshabilitado:
 * el navegador bloqueará peticiones desde cualquier origen distinto al propio.</p>
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    /**
     * Fuente de configuración CORS aplicada por el filtro de seguridad.
     * Sin orígenes configurados devuelve {@code null} por petición,
     * lo que a nivel práctico desactiva CORS.
     * @param properties propiedades CORS enlazadas desde la configuración
     * @return fuente de configuración CORS para todos los endpoints
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        if (CollectionUtils.isEmpty(properties.allowedOrigins())) {
            // CORS deshabilitado (por defecto en producción)
            return request -> null;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        // Solo se sobreescriben los defaults de Spring si la propiedad está definida
        if (properties.allowedMethods() != null) {
            configuration.setAllowedMethods(properties.allowedMethods());
        }
        if (properties.allowedHeaders() != null) {
            configuration.setAllowedHeaders(properties.allowedHeaders());
        }
        configuration.setAllowCredentials(Boolean.TRUE.equals(properties.allowCredentials()));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
