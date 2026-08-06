package com.athletecore.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propiedades de configuración del módulo de entrenamientos.
 * Prefijo: training.attendance
 */
@ConfigurationProperties(prefix = "training.attendance")
public record TrainingProperties(
        /**
         * Umbral de ausencias consecutivas que dispara una alerta por deportista.
         * Propiedad: training.attendance.absence-threshold (default 3).
         */
        @DefaultValue("3") int absenceThreshold
) {
}
