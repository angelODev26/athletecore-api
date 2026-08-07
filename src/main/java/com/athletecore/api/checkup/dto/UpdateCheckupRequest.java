package com.athletecore.api.checkup.dto;

import java.util.Locale;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para actualizar un chequeo. Todos los campos son opcionales
 * (actualización parcial); el servicio de Fase C decide la semántica de merge.
 */
public record UpdateCheckupRequest(
    @Min(value = 1900, message = "El año debe estar entre 1900 y 2100")
    @Max(value = 2100, message = "El año debe estar entre 1900 y 2100")
    Integer year,

    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    Integer month,

    @Pattern(regexp = "^(INFANTIL|JUVENIL|MAYOR)$",
            message = "Categoría no válida (INFANTIL, JUVENIL o MAYOR)")
    @Size(max = 30, message = "La categoría no puede superar 30 caracteres")
    String category,

    String notes
) {
    /**
     * Normaliza la categoría a mayúsculas (p.ej. "mayor" -> "MAYOR") antes de la validación.
     */
    public UpdateCheckupRequest {
        if (category != null) {
            category = category.trim().toUpperCase(Locale.ROOT);
        }
    }
}
