package com.athletecore.api.checkup.dto;

import java.util.Locale;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para crear un chequeo mensual de rendimiento.
 */
public record CreateCheckupRequest(
    @NotNull(message = "El ID del deportista es obligatorio")
    @Positive(message = "El ID del deportista debe ser positivo")
    Long athleteId,

    @NotNull(message = "El año es obligatorio")
    @Min(value = 1900, message = "El año debe estar entre 1900 y 2100")
    @Max(value = 2100, message = "El año debe estar entre 1900 y 2100")
    Integer year,

    @NotNull(message = "El mes es obligatorio")
    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    Integer month,

    @NotBlank(message = "La categoría es obligatoria")
    @Pattern(regexp = "^(INFANTIL|JUVENIL|MAYOR)$",
            message = "Categoría no válida (INFANTIL, JUVENIL o MAYOR)")
    @Size(max = 30, message = "La categoría no puede superar 30 caracteres")
    String category,

    String notes
) {
    /**
     * Normaliza la categoría a mayúsculas (p.ej. "mayor" -> "MAYOR") antes de la validación.
     */
    public CreateCheckupRequest {
        if (category != null) {
            category = category.trim().toUpperCase(Locale.ROOT);
        }
    }
}
