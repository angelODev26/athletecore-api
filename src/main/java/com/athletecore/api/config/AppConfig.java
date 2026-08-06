package com.athletecore.api.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración general de aplicación para beans transversales.
 * Registra el bean Clock usado por la lógica de alertas (determinista y testeable)
 * y habilita las propiedades de configuración del módulo de entrenamientos.
 */
@Configuration
@EnableConfigurationProperties(TrainingProperties.class)
public class AppConfig {

    /**
     * Reloj del sistema. Inyectado en servicios de negocio para que la lógica
     * dependiente de la fecha actual sea determinista y testeable con Clock fijo.
     * @return Clock de la zona horaria por defecto del sistema
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
