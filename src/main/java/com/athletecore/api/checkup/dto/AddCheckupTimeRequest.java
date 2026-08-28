package com.athletecore.api.checkup.dto;

import java.math.BigDecimal;
import java.util.Locale;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para registrar un tiempo de prueba en un chequeo.
 * El tiempo se envía en segundos decimales (p.ej. 65.250); la presentación
 * mm:ss.ms se genera en los DTO de respuesta vía TimeFormatter.
 */
public record AddCheckupTimeRequest(
    @NotBlank(message = "El estilo es obligatorio")
    @Pattern(regexp = "^(LIBRE|ESPALDA|BRAZA|MARIPOSA|COMBINADO)$",
            message = "Estilo no válido (LIBRE, ESPALDA, BRAZA, MARIPOSA o COMBINADO)")
    @Size(max = 20, message = "El estilo no puede superar 20 caracteres")
    String style,

    @NotNull(message = "La distancia es obligatoria")
    @Min(value = 1, message = "La distancia debe ser mayor a 0")
    Integer distance,

    @NotNull(message = "El tiempo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El tiempo debe ser mayor a 0")
    BigDecimal timeSeconds
) {
    /**
     * Normaliza el estilo a mayúsculas (p.ej. "libre" -> "LIBRE") antes de la validación.
     */
    public AddCheckupTimeRequest {
        if (style != null) {
            style = style.trim().toUpperCase(Locale.ROOT);
        }
    }
}
