package com.athletecore.api.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración general de aplicación para beans transversales.
 * Registra el bean Clock usado por la lógica de alertas (determinista y testeable),
 * habilita las propiedades de configuración del módulo de entrenamientos y activa
 * la programación automática de tareas (módulo de reportes).
 */
@Configuration
@EnableConfigurationProperties(TrainingProperties.class)
@EnableScheduling
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
