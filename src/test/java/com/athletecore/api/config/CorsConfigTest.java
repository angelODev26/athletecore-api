package com.athletecore.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Tests unitarios de {@link CorsConfig}: verifican la bifurcación crítica de seguridad
 * entre CORS habilitado (dev, orígenes explícitos) y CORS deshabilitado (prod, sin orígenes).
 * No elevan contexto Spring: se instancian los objetos directamente.
 */
class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();

    @Test
    @DisplayName("Sin orígenes configurados (prod) devuelve una fuente que no emite configuración CORS")
    void sinOrigenesCorSLaFuenteDevuelveNull() {
        CorsProperties properties = new CorsProperties(List.of(), null, null, null);

        CorsConfigurationSource source = corsConfig.corsConfigurationSource(properties);

        assertNull(source.getCorsConfiguration(new MockHttpServletRequest()),
                "Sin orígenes, CORS debe quedar deshabilitado");
    }

    @Test
    @DisplayName("Orígenes nulos (prod sin la propiedad) también deshabilitan CORS")
    void origenesNulosTambienDeshabilitanCors() {
        CorsProperties properties = new CorsProperties(null, null, null, null);

        CorsConfigurationSource source = corsConfig.corsConfigurationSource(properties);

        assertNull(source.getCorsConfiguration(new MockHttpServletRequest()));
    }

    @Test
    @DisplayName("Con orígenes configurados (dev) se acepta el origen del frontend y se rechazan otros")
    void conOrigenesSePermiteElFrontendYSeRechazanOtros() {
        CorsProperties properties = new CorsProperties(
                List.of("http://localhost:5173", "http://localhost:3000"),
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"),
                List.of("*"),
                true);

        CorsConfigurationSource source = corsConfig.corsConfigurationSource(properties);
        CorsConfiguration configuration =
                source.getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(configuration);
        assertEquals("http://localhost:5173", configuration.checkOrigin("http://localhost:5173"));
        assertEquals("http://localhost:3000", configuration.checkOrigin("http://localhost:3000"));
        assertNull(configuration.checkOrigin("https://evil.com"),
                "Un origen no listado debe ser rechazado");
        assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
    }

    @Test
    @DisplayName("Sin allowCredentials configurado el default es false (nunca credenciales por defecto)")
    void sinAllowCredentialsElDefaultEsFalse() {
        CorsProperties properties = new CorsProperties(List.of("http://localhost:5173"), null, null, null);

        CorsConfiguration configuration = corsConfig.corsConfigurationSource(properties)
                .getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(configuration);
        assertFalse(Boolean.TRUE.equals(configuration.getAllowCredentials()));
    }
}
